package testsupport.adapters

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.tamj0rd2.domain.*
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.webapp.*
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.Websocket
import org.slf4j.Logger
import testsupport.ApplicationDriver
import java.time.Instant.now
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class WebsocketDriver(host: String, ackTimeoutMs: Long = 300) :
    ApplicationDriver {
    private val wsBaseUrl = "ws://$host"
    private val answerTracker = AnswerTracker(ackTimeoutMs)

    private var playerId = PlayerId.unidentified
    private lateinit var logger: Logger
    private var ws: Websocket

    override lateinit var state: PlayerState

    init {
        identifyAs(PlayerId.unidentified)

        val syncer = Syncer()
        var connected = false

        ws = WebsocketClient.nonBlocking(
            Uri.of("$wsBaseUrl/play"),
            onError = { throw it },
            timeout = 1.seconds.toJavaDuration(),
        ) {
            connected = true
            syncer.wake()
        }

        ws.onMessage {
            val message = messageLens(it)
            logger.receivedMessage(message)
            handleMessage(message)
        }
        ws.onClose { logger.warn("websocket connection closed") }
        syncer.waitUntil(ackTimeoutMs) { connected }
    }

    override fun perform(command: PlayerCommand): Result<Unit, CommandError> {
        val message = Message.ToServer(command)
        val nackReason = answerTracker.waitForAnswer(message.id) {
            logger.sending(message)
            ws.send(messageLens(message))
            logger.awaitingAck(message)
        }

        if (nackReason == null) {
            logger.info("success: $command")
            return Ok(Unit)
        }

        GameErrorCode.fromString(nackReason).throwException()
    }

    private fun identifyAs(playerId: PlayerId) {
        this.playerId = playerId
        this.logger = playerId.logger("wsClient")
    }

    private fun handleMessage(message: Message) {
        when (message) {
            is Message.AcceptanceFromServer -> {
                state = message.state
                answerTracker.markAsAccepted(message.id)
                logger.processedMessage(message)
            }

            is Message.Rejection -> {
                answerTracker.markAsRejected(message.id, message.reason)
            }

            is Message.ToClient -> {
                state = message.state
                logger.processedMessage(message)

                val response = message.accept()
                logger.sending(response)
                ws.send(messageLens(response))
                logger.sentMessage(response)
            }

            is Message.KeepAlive -> {}

            else -> error("invalid message from server to client: $message")
        }
    }
}

class Syncer {
    private val syncObject = Object()

    fun wake() = synchronized(syncObject) { syncObject.notify() }

    fun waitUntil(timeoutMs: Long, backoffMs: Long = timeoutMs / 5, predicate: () -> Boolean) {
        synchronized(syncObject) {
            val mustEndBy = now().plusMillis(timeoutMs)
            do {
                if (predicate()) return@synchronized
                syncObject.wait(backoffMs)
            } while (mustEndBy > now())

            error("predicate never succeeded")
        }
    }
}
