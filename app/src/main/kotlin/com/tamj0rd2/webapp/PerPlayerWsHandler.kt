package com.tamj0rd2.webapp

import com.github.michaelbull.result.getOrThrow
import com.tamj0rd2.domain.*
import com.tamj0rd2.messaging.Message.*
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

                if (triggeredBy == playerId) {
                    return@subscribeToGameEvents
                }

                val otwMessage = ToClient(state)
                val nackReason = answerTracker.waitForAnswer(otwMessage.id) {
                    logger.sending(otwMessage)
                    ws.send(messageLens(otwMessage))
                    logger.awaitingAck(otwMessage)
                }
                if (nackReason != null) error("client nacked the message which should never happpend - $nackReason")
            }
        }

        val response = runCatching {
            game.perform(message.command).getOrThrow {
                logger.error("$it")
                it.reason.asException()
            }
        }.fold(
            onSuccess = {
                logger.processedMessage(message)
                message.accept(state)
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
