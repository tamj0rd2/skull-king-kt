package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameErrorCodeException
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.messaging.Notification
import com.tamj0rd2.messaging.Message.*
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.time.Duration

private const val serverIdleTimeout = 30000L

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
            var playerId = PlayerId.unidentified
            lateinit var logger: Logger

            fun identifyAs(id: PlayerId) {
                playerId = id
                logger = LoggerFactory.getLogger("$playerId:wsHandler")
            }

            identifyAs(PlayerId.unidentified)

            WsResponse { ws ->
                val acknowledgements = Acknowledgements(acknowledgementTimeoutMs)
                val messagesToClient = LockedValue<List<Notification>>()

                var isConnected = true
                ws.onClose {
                    isConnected = false
                    logger.warn("$playerId disconnected - ${it.description}")
                }
                ws.onError { logger.error(it.message ?: it::class.simpleName ?: "unknown error", it) }

                ws.onMessage {
                    val message = messageLens(it)
                    logger.receivedMessage(message)

                    when (message) {
                        is AckFromClient -> {
                            acknowledgements.ack(message.id)
                            logger.processedMessage(message)
                        }
                        // TODO: this desperately needs refactoring
                        is ToServer -> {
                            if (message.command is PlayerCommand.JoinGame) {
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
                                    val nackReason = acknowledgements.waitFor(otwMessage.id) {
                                        logger.sending(otwMessage)
                                        ws.send(messageLens(otwMessage))
                                        logger.awaitingAck(otwMessage)
                                    }
                                    if (nackReason != null) error("client nacked the message which should never happpend - $nackReason")
                                }
                            }

                            messagesToClient.use {
                                val response = runCatching { game.perform(message.command) }.fold(
                                    onSuccess = {
                                        logger.processedMessage(message)
                                        message.acknowledge(lockedValue.orEmpty())
                                    },
                                    onFailure = {
                                        logger.error("processing message failed - $message", it)
                                        when(it) {
                                            is GameErrorCodeException -> message.nack(it.errorCode)
                                            else -> message.nack(it.message ?: it::class.simpleName ?: "unknown error")
                                        }
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

                val timer = Timer()
                timer.schedule(timerTask {
                    if (!isConnected) {
                        timer.cancel()
                        return@timerTask
                    }
                    ws.send(messageLens(KeepAlive()))
                }, 0, serverIdleTimeout / 2)

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
        is GameEvent.GameCompleted -> listOf(Notification.GameCompleted)
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

