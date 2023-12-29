package com.tamj0rd2.webapp

import com.github.michaelbull.result.getOrThrow
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameErrorCodeException
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerGameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.messaging.Message.*
import com.tamj0rd2.messaging.Notification
import org.http4k.routing.static
import org.http4k.websocket.Websocket
import java.util.*
import kotlin.concurrent.timerTask

private const val serverIdleTimeout = 30000L

internal class PerPlayerWsHandler(
    private val ws: Websocket,
    private val game: Game,
    acknowledgementTimeoutMs: Long
) {
    private val loggingContext = "wsHandler"
    private var playerId = PlayerId.unidentified
        set(value) {
            field = value
            logger = field.logger(loggingContext)
        }
    private lateinit var state: PlayerGameState
    private var logger = playerId.logger(loggingContext)
    private var isConnected = true
    private val answerTracker = AnswerTracker(acknowledgementTimeoutMs)
    private val messagesToClient = LockedValue<List<Notification>>() // TODO: wtf is this? I can't remember...

    init {
        ws.onError { e ->
            val errorMessage = e.message ?: e::class.simpleName ?: "unknown error"
            logger.error(errorMessage, e)
        }

        ws.onMessage {
            val message = messageLens(it)
            logger.receivedMessage(message)
            when (message) {
                is AcceptanceFromClient -> handleAcceptanceFromClient(message)
                is ToServer -> handleMessageToServer(message)
                else -> error("invalid message from client to server: $message")
            }
        }

        ws.onClose {
            isConnected = false
            logger.warn("disconnected - ${it.description}")
        }

        logger.info("connected")
        keepConnectionAlive()
    }

    private fun keepConnectionAlive() {
        val timer = Timer()
        timer.schedule(timerTask {
            if (isConnected) ws.send(messageLens(KeepAlive())) else timer.cancel()
        }, 0, serverIdleTimeout / 2)
    }

    private fun handleAcceptanceFromClient(message: AcceptanceFromClient) {
        answerTracker.markAsAccepted(message.id)
        logger.processedMessage(message)
    }

    private fun handleMessageToServer(message: ToServer) {
        if (message.command is PlayerCommand.JoinGame) {
            playerId = message.command.actor
            state = PlayerGameState.ofPlayer(playerId, game.allEventsSoFar)

            game.subscribeToGameEvents { events, triggeredBy ->
                state = state.handle(events)

                val messages = events
                    .flatMap { it.notifications(game, playerId) }
                    .ifEmpty { return@subscribeToGameEvents }

                if (triggeredBy == playerId) {
                    messagesToClient.lockedValue = messages
                    return@subscribeToGameEvents
                }

                val otwMessage = ToClient(messages, state)
                val nackReason = answerTracker.waitForAnswer(otwMessage.id) {
                    logger.sending(otwMessage)
                    ws.send(messageLens(otwMessage))
                    logger.awaitingAck(otwMessage)
                }
                if (nackReason != null) error("client nacked the message which should never happpend - $nackReason")
            }
        }

        messagesToClient.use {
            val response = runCatching {
                game.perform(message.command).getOrThrow {
                    logger.error("$it")
                    it.reason.asException()
                }
            }.fold(
                onSuccess = {
                    logger.processedMessage(message)
                    message.accept(lockedValue.orEmpty(), state)
                },
                onFailure = {
                    logger.error("processing message failed - $message", it)
                    when (it) {
                        is GameErrorCodeException -> message.reject(it.errorCode)
                        else -> message.reject(it.message ?: it::class.simpleName ?: "unknown error")
                    }
                }
            )

            logger.sending(response)
            ws.send(messageLens(response))
            logger.sentMessage(response)
        }
    }
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
                messages.add(Notification.YourTurn(game.getCardsInHand(thisPlayerId)!!))
            }

            messages
        }

        // TODO: do something here
        is GameEvent.CardsDealt -> listOf()
        is GameEvent.GameCompleted -> listOf(Notification.GameCompleted)
        is GameEvent.GameStarted -> listOf(Notification.GameStarted(players))
        is GameEvent.PlayerJoined -> buildList {
            val waitingForMorePlayers = game.state == GameState.WaitingForMorePlayers
            if (playerId == thisPlayerId) add(Notification.YouJoined(playerId, game.players, waitingForMorePlayers))
            else add(Notification.PlayerJoined(playerId, waitingForMorePlayers))
        }

        is GameEvent.RoundStarted -> listOf(
            Notification.RoundStarted(
                game.getCardsInHand(thisPlayerId)!!,
                roundNumber
            )
        )

        is GameEvent.TrickCompleted -> {
            val messages = mutableListOf<Notification>(
                Notification.TrickCompleted(winner)
            )

            if (game.trickNumber.value == game.roundNumber.value) {
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
                messages.add(Notification.YourTurn(game.getCardsInHand(thisPlayerId)!!))
            }

            messages
        }

        // TODO; do something here
        is GameEvent.SuitEstablished -> emptyList()
    }