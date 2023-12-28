package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameErrorCodeException
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.messaging.Message.*
import com.tamj0rd2.messaging.Notification
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

            game.subscribeToGameEvents { events, triggeredBy ->
                val messages = events
                    .flatMap { it.asNotifications(PlayerGameState(game, playerId)) }
                    .ifEmpty { return@subscribeToGameEvents }

                if (triggeredBy == playerId) {
                    messagesToClient.lockedValue = messages
                    return@subscribeToGameEvents
                }

                val otwMessage = ToClient(messages)
                val nackReason = answerTracker.waitForAnswer(otwMessage.id) {
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
                    message.accept(lockedValue.orEmpty())
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

// NOTE: this was my attempt to decrease the amount of things that directly depend on the Game class
private data class PlayerGameState(
    val myPlayerId: PlayerId,
    val currentPlayersTurn: PlayerId?,
    val isMyTurn: Boolean,
    val amWaitingForMorePlayers: Boolean,
    val allPlayers: List<PlayerId>,
    val isLastTrick: Boolean,
    val winsOfTheRound: Map<PlayerId, Int>,
    private val cards: List<CardWithPlayability>?,
) {
    constructor(game: Game, playerId: PlayerId) : this(
        myPlayerId = playerId,
        currentPlayersTurn = game.currentPlayersTurn,
        isMyTurn = game.currentPlayersTurn == playerId,
        amWaitingForMorePlayers = game.state == GameState.WaitingForMorePlayers,
        allPlayers = game.players,
        isLastTrick = game.trickNumber.value == game.roundNumber.value,
        winsOfTheRound = game.winsOfTheRound,
        cards = game.getCardsInHand(playerId),
    )

    val cardsInMyHand get(): List<CardWithPlayability> {
        val cards = cards
        requireNotNull(cards) { "$myPlayerId has no hand at all. Is the game in the correct state?" }
        return cards
    }
}

private fun GameEvent.asNotifications(state: PlayerGameState): List<Notification> =
    when (this) {
        is GameEvent.BidPlaced -> listOf(Notification.BidPlaced(playerId))
        is GameEvent.BiddingCompleted -> listOf(Notification.BiddingCompleted(bids))
        is GameEvent.CardPlayed -> buildList {
            add(
                Notification.CardPlayed(
                    playerId = playerId,
                    card = card,
                    nextPlayer = state.currentPlayersTurn
                )
            )

            if (state.isMyTurn) add(Notification.YourTurn(state.cardsInMyHand))
        }

        is GameEvent.CardsDealt -> {
            // TODO: use the cards dealt event and then remove the other places where the same information is sent
            emptyList()
        }
        is GameEvent.GameCompleted -> listOf(Notification.GameCompleted)
        is GameEvent.GameStarted -> listOf(Notification.GameStarted(players))
        is GameEvent.PlayerJoined -> buildList {
            if (playerId == state.myPlayerId) add(
                Notification.YouJoined(
                    playerId,
                    state.allPlayers,
                    state.amWaitingForMorePlayers
                )
            )
            else add(Notification.PlayerJoined(playerId, state.amWaitingForMorePlayers))
        }

        is GameEvent.RoundStarted -> listOf(
            Notification.RoundStarted(state.cardsInMyHand, roundNumber)
        )

        is GameEvent.TrickCompleted -> buildList {
            add(Notification.TrickCompleted(winner))

            if (state.isLastTrick) add(Notification.RoundCompleted(state.winsOfTheRound))
        }

        is GameEvent.TrickStarted -> buildList {
            add(
                Notification.TrickStarted(
                    trickNumber = trickNumber,
                    firstPlayer = state.currentPlayersTurn ?: error("currentPlayer is null")
                )
            )

            if (state.isMyTurn) add(Notification.YourTurn(state.cardsInMyHand))
        }
    }

