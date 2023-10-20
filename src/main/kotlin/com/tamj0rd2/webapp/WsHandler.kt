package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.lens.Path
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.slf4j.LoggerFactory
import java.time.Instant.now
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.timerTask
import kotlin.time.Duration

internal fun wsHandler(
    game: Game,
    automateGameMasterCommands: Boolean,
    automaticGameMasterDelayOverride: Duration?
): RoutingWsHandler {
    val playerIdLens = Path.map(::PlayerId, PlayerId::playerId).of("playerId")
    val clientMessageLens = WsMessage.auto<MessageFromClient>().toLens()

    if (automateGameMasterCommands) {
        AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
    }

    return websockets(
        "/{playerId}" bind { req ->
            val playerId = playerIdLens(req)
            val logger = LoggerFactory.getLogger("$playerId:wsHandler")
            val syncObject = Object()
            val acknowledgements = mutableSetOf<UUID>()

            fun waitForAcknowledgement(message: MessageToClient.MessageRequiringAcknowledgement) {
                synchronized(syncObject) {
                    logger.info("waiting for acknowledgement of ${message.messageId} $message")
                    val mustEndBy = now().plusSeconds(1)
                    val messageHasBeenAcknowledged = { acknowledgements.contains(message.messageId) }

                    do { syncObject.wait(100) } while (now() < mustEndBy && !messageHasBeenAcknowledged())

                    if (messageHasBeenAcknowledged())
                        acknowledgements.remove(message.messageId)
                    else
                        error("$message ${message.messageId} to $playerId was not acknowledged within the timeout")
                }
            }

            WsResponse { ws ->
                game.subscribeToGameEvents { event ->
                    val message = event.asMessageToClient(game, playerId)

                    logger.info("sending message to $playerId: $message")
                    ws.send(messageToClientLens(message))

                    when (message) {
                        is MessageToClient.MessageRequiringAcknowledgement -> waitForAcknowledgement(message)
                        is MessageToClient.Multi -> message.messagesRequiringAcknowledgement().forEach(::waitForAcknowledgement)
                        else -> {}
                    }
                }

                ws.onMessage {
                    val message = clientMessageLens(it)
                    logger.info("received $message")

                    when (message) {
                        is MessageFromClient.BidPlaced -> game.bid(playerId, message.bid)
                        is MessageFromClient.UnhandledServerMessage -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                        is MessageFromClient.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                        is MessageFromClient.CardPlayed -> game.playCard(playerId, message.cardName)
                        is MessageFromClient.Acknowledgement -> synchronized(syncObject) {
                            acknowledgements.add(message.id)
                            syncObject.notify()
                        }
                    }

                    if (message is MessageFromClient.MessageRequiringAcknowledgement) {
                        ws.send(messageToClientLens(message.acknowledge()))
                    }
                }

                logger.info("connected")
                ws.send(
                    messageToClientLens(
                        MessageToClient.YouJoined(
                            game.players,
                            game.isInState(GameState.WaitingForMorePlayers)
                        )
                    )
                )
            }
        }
    )
}

fun GameEvent.asMessageToClient(game: Game, thisPlayerId: PlayerId): MessageToClient {
    return when (this) {
        is GameEvent.BidPlaced -> MessageToClient.BidPlaced(playerId)
        is GameEvent.BiddingCompleted -> MessageToClient.BiddingCompleted(bids)
        is GameEvent.CardPlayed -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.CardPlayed(
                    playerId = playerId,
                    card = card,
                    nextPlayer = game.currentPlayersTurn
                ),
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(MessageToClient.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            MessageToClient.Multi(messages)
        }

        is GameEvent.CardsDealt -> TODO("add cards dealt event")
        is GameEvent.GameCompleted -> MessageToClient.GameCompleted
        is GameEvent.GameStarted -> MessageToClient.GameStarted(players)
        is GameEvent.PlayerJoined -> MessageToClient.PlayerJoined(
            playerId,
            game.isInState(GameState.WaitingForMorePlayers)
        )

        is GameEvent.RoundStarted -> MessageToClient.RoundStarted(
            game.getCardsInHand(thisPlayerId),
            roundNumber
        )

        is GameEvent.TrickCompleted -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.TrickCompleted(winner)
            )

            if (game.trickNumber == game.roundNumber) {
                messages += MessageToClient.RoundCompleted(game.winsOfTheRound)
            }

            MessageToClient.Multi(messages)
        }

        is GameEvent.TrickStarted -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.TrickStarted(
                    trickNumber,
                    game.currentPlayersTurn ?: error("currentPlayer is null")
                )
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(MessageToClient.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            MessageToClient.Multi(messages)
        }
    }
}

private class AutomatedGameMaster(private val game: Game, private val delayOverride: Duration?) {
    private val allGameEvents = CopyOnWriteArrayList<GameEvent>()
    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    fun start() {
        val timer = Timer()

        game.subscribeToGameEvents { event ->
            allGameEvents.add(event)

            when (event) {
                is GameEvent.PlayerJoined -> {
                    if (game.state != GameState.WaitingToStart) return@subscribeToGameEvents
                    timer.schedule(timerTask {
                        if (allGameEvents.last() != event) return@timerTask

                        logger.info("Starting the game")
                        game.start()
                    }, delayOverride?.inWholeMilliseconds ?: 5000)
                }

                is GameEvent.BiddingCompleted -> {
                    timer.schedule(timerTask {
                        val lastEvent = allGameEvents.last()
                        require(lastEvent == event) { "last event was not bidding completed, it was $lastEvent" }

                        logger.info("Starting the first trick")
                        game.startNextTrick()
                    }, delayOverride?.inWholeMilliseconds ?: 3000)
                }

                is GameEvent.TrickCompleted -> {
                    timer.schedule(timerTask {
                        val lastEvent = allGameEvents.last()
                        require(lastEvent == event) { "last event was not trick completed, it was $lastEvent" }

                        // TODO: need to write a test for what happens after round 10 trick 10
                        if (game.roundNumber == game.trickNumber) {
                            logger.info("Starting the next round")
                            game.startNextRound()
                        } else {
                            logger.info("Starting the next trick")
                            game.startNextTrick()
                        }
                    }, delayOverride?.inWholeMilliseconds ?: 3000)
                }

                else -> {}
            }
        }
    }
}
