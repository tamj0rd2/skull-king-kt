package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Command.GameMasterCommand.*
import com.tamj0rd2.domain.Command.PlayerCommand.JoinGame
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.Message.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.timerTask
import kotlin.time.Duration

class LockedValue<T> {
    private val lock = Mutex()

    var lockedValue: T? = null
        set(newValue) {
            require(lock.isLocked) { "cannot set the value without first initialising it with use()" }
            field = newValue
        }

    fun use(initialValue: T? = null, block: LockedValue<T>.() -> Unit) {
        require(lockedValue == null) { "the locked value is already erroneously set" }

        runBlocking {
            lock.withLock(this) {
                lockedValue = initialValue
                try {
                    block()
                } finally {
                    lockedValue = null
                }
            }
        }
    }
}

internal fun wsHandler(
    game: Game,
    automateGameMasterCommands: Boolean,
    automaticGameMasterDelayOverride: Duration?,
    acknowledgementTimeoutMs: Long,
): RoutingWsHandler {
    if (automateGameMasterCommands) {
        AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
    }

    return websockets(
        "/play" bind { _ ->
            lateinit var playerId: PlayerId
            lateinit var logger: Logger

            fun identifyAs(id: PlayerId) {
                playerId = id
                logger = LoggerFactory.getLogger("$playerId:wsHandler")
            }

            identifyAs(PlayerId.unidentified)

            WsResponse { ws ->
                val acknowledgements = Acknowledgements(acknowledgementTimeoutMs)
                val messagesToClient = LockedValue<List<Notification>>()

                ws.onClose { logger.warn("$playerId disconnected") }
                ws.onError { logger.error(it.message, it) }

                ws.onMessage {
                    val message = messageLens(it)
                    logger.receivedMessage(message)

                    when (message) {
                        is Ack.FromClient -> {
                            acknowledgements.ack(message.id)
                            logger.processedMessage(message)
                        }
                        // TODO: this desperately needs refactoring
                        is ToServer -> {
                            if (message.command is JoinGame) {
                                identifyAs(message.command.actor)

                                game.subscribeToGameEvents { events, triggeredBy ->
                                    val messages = events
                                        .flatMap { it.notifications(game, playerId) }
                                        .ifEmpty { return@subscribeToGameEvents }

                                    if (triggeredBy == playerId) {
                                        messagesToClient.lockedValue = messages
                                        return@subscribeToGameEvents
                                    }

                                    val otwMessage = ToClient(messages)
                                    val wasSuccessful = acknowledgements.waitFor(otwMessage.id) {
                                        logger.sending(otwMessage)
                                        ws.send(messageLens(otwMessage))
                                        logger.awaitingAck(otwMessage)
                                    }
                                    if (!wasSuccessful) error("client nacked the message")
                                }
                            }

                            messagesToClient.use {
                                val response = runCatching { game.perform(message.command) }.fold(
                                    onSuccess = {
                                        logger.processedMessage(message)
                                        message.acknowledge(lockedValue.orEmpty())
                                    },
                                    onFailure = {
                                        logger.error("processing message failed - $message - $it")
                                        message.nack()
                                    }
                                )

                                logger.sending(response)
                                ws.send(messageLens(response))
                                logger.sentMessage(response)
                            }
                        }

                        else -> error("invalid message from client to server: $message")
                    }
                }

                logger.info("connected")
            }
        }
    )
}

fun GameEvent.notifications(game: Game, thisPlayerId: PlayerId): List<Notification> =
    when (this) {
        is GameEvent.BidPlaced -> listOf(Notification.BidPlaced(playerId))
        is GameEvent.BiddingCompleted -> listOf(Notification.BiddingCompleted(bids))
        is GameEvent.CardPlayed -> {
            val messages = mutableListOf<Notification>(
                Notification.CardPlayed(
                    playerId = playerId,
                    card = card,
                    nextPlayer = game.currentPlayersTurn
                ),
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(Notification.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            messages
        }

        is GameEvent.CardsDealt -> TODO("add cards dealt event")
        is GameEvent.GameCompleted -> listOf(Notification.GameCompleted())
        is GameEvent.GameStarted -> listOf(Notification.GameStarted(players))
        is GameEvent.PlayerJoined -> buildList {
            val waitingForMorePlayers = game.isInState(GameState.WaitingForMorePlayers)
            if (playerId == thisPlayerId) add(Notification.YouJoined(playerId, game.players, waitingForMorePlayers))
            else add(Notification.PlayerJoined(playerId, waitingForMorePlayers))
        }

        is GameEvent.RoundStarted -> listOf(
            Notification.RoundStarted(
                game.getCardsInHand(thisPlayerId),
                roundNumber
            )
        )

        is GameEvent.TrickCompleted -> {
            val messages = mutableListOf<Notification>(
                Notification.TrickCompleted(winner)
            )

            if (game.trickNumber == game.roundNumber) {
                messages += Notification.RoundCompleted(game.winsOfTheRound)
            }

            messages
        }

        is GameEvent.TrickStarted -> {
            val messages = mutableListOf<Notification>(
                Notification.TrickStarted(
                    trickNumber,
                    game.currentPlayersTurn ?: error("currentPlayer is null")
                )
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(Notification.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            messages
        }
    }

private class AutomatedGameMaster(private val game: Game, private val delayOverride: Duration?) {
    private val allGameEvents = CopyOnWriteArrayList<GameEvent>()
    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    fun start() {
        val timer = Timer()

        game.subscribeToGameEvents { events, _ ->
            events.forEach { event ->
                allGameEvents.add(event)

                when (event) {
                    is GameEvent.PlayerJoined -> {
                        if (game.state != GameState.WaitingToStart) return@subscribeToGameEvents
                        timer.schedule(timerTask {
                            if (allGameEvents.last() != event) return@timerTask

                            logger.info("Starting the game")
                            game.perform(StartGame)
                        }, delayOverride?.inWholeMilliseconds ?: 5000)
                    }

                    is GameEvent.BiddingCompleted -> {
                        timer.schedule(timerTask {
                            val lastEvent = allGameEvents.last()
                            require(lastEvent == event) { "last event was not bidding completed, it was $lastEvent" }

                            logger.info("Starting the first trick")
                            game.perform(StartNextTrick)
                        }, delayOverride?.inWholeMilliseconds ?: 3000)
                    }

                    is GameEvent.TrickCompleted -> {
                        timer.schedule(timerTask {
                            val lastEvent = allGameEvents.last()
                            require(lastEvent == event) { "last event was not trick completed, it was $lastEvent" }

                            // TODO: need to write a test for what happens after round 10 trick 10
                            if (game.roundNumber == game.trickNumber) {
                                logger.info("Starting the next round")
                                game.perform(StartNextRound)
                            } else {
                                logger.info("Starting the next trick")
                                game.perform(StartNextTrick)
                            }
                        }, delayOverride?.inWholeMilliseconds ?: 3000)
                    }

                    else -> {}
                }
            }
        }
    }
}
