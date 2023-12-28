package testsupport.adapters

import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber
import com.tamj0rd2.webapp.AnswerTracker
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.messaging.Notification
import com.tamj0rd2.webapp.messageLens
import com.tamj0rd2.webapp.awaitingAck
import com.tamj0rd2.webapp.logger
import com.tamj0rd2.webapp.processedMessage
import com.tamj0rd2.webapp.receivedMessage
import com.tamj0rd2.webapp.sending
import com.tamj0rd2.webapp.sentMessage
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

    override var winsOfTheRound: Map<PlayerId, Int> = emptyMap()
    override var trickWinner: PlayerId? = null
    override var currentPlayer: PlayerId? = null
    override var trickNumber: TrickNumber? = null
    override var roundNumber: RoundNumber? = null
    override val trick = mutableListOf<PlayedCard>()
    override var roundPhase: RoundPhase? = null
    override var gameState: GameState? = null
    override var playersInRoom = mutableListOf<PlayerId>()
    override var hand = mutableListOf<CardWithPlayability>()
    override var bids = mutableMapOf<PlayerId, DisplayBid>()

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

            when (message) {
                is Message.AcceptanceFromServer -> {
                    message.notifications.forEach(::handleMessage)
                    answerTracker.markAsAccepted(message.id)
                    logger.processedMessage(message)
                }

                is Message.Rejection -> {
                    answerTracker.markAsRejected(message.id, message.reason)
                }

                is Message.ToClient -> {
                    message.notifications.forEach(::handleMessage)
                    logger.processedMessage(message)


                    message.accept().let { response ->
                        logger.sending(response)
                        ws.send(messageLens(response))
                        logger.sentMessage(response)
                    }
                }

                is Message.KeepAlive -> {}

                else -> error("invalid message from server to client: $message")
            }
        }

        ws.onClose { logger.warn("websocket connection closed") }
        syncer.waitUntil(ackTimeoutMs) { connected }
    }

    override fun perform(command: PlayerCommand) {
        val message = Message.ToServer(command)
        val nackReason = answerTracker.waitForAnswer(message.id) {
            logger.sending(message)
            ws.send(messageLens(message))
            logger.awaitingAck(message)
        }

        if (nackReason == null) {
            logger.info("success: $command")
            return
        }

        GameErrorCode.fromString(nackReason).throwException()
    }

    private fun identifyAs(playerId: PlayerId) {
        this.playerId = playerId
        this.logger = playerId.logger("wsClient")
    }

    private fun handleMessage(message: Notification) {
        when (message) {
            is Notification.BidPlaced -> {
                bids[message.playerId] = DisplayBid.Hidden
            }

            is Notification.BiddingCompleted -> {
                roundPhase = RoundPhase.BiddingCompleted
                bids = message.bids.mapValues { (_, bid) -> DisplayBid.Placed(bid) }.toMutableMap()
            }

            is Notification.CardPlayed -> {
                currentPlayer = message.nextPlayer
                trick.add(message.card.playedBy(message.playerId))
                if (message.playerId == playerId) {
                    hand.removeFirstIf { it.card == message.card }
                }
            }

            is Notification.GameCompleted -> {
                gameState = GameState.Complete
            }

            is Notification.GameStarted -> {
                playersInRoom = message.players.toMutableList()
                gameState = GameState.InProgress
            }

            is Notification.PlayerJoined -> {
                playersInRoom.add(message.playerId)
                gameState =
                    if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is Notification.RoundCompleted -> {
                winsOfTheRound = message.wins
            }

            is Notification.RoundStarted -> {
                roundNumber = message.roundNumber
                roundPhase = RoundPhase.Bidding
                trickNumber = null
                hand = message.cardsDealt.toMutableList()
                bids = playersInRoom.associateWith { DisplayBid.None }.toMutableMap()
            }

            is Notification.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
                trickWinner = message.winner
            }

            is Notification.TrickStarted -> {
                roundPhase = RoundPhase.TrickTaking
                trickNumber = message.trickNumber
                currentPlayer = message.firstPlayer
                trick.clear()
            }

            is Notification.YouJoined -> {
                identifyAs(message.playerId)
                playersInRoom = message.players.toMutableList()
                gameState =
                    if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is Notification.YourTurn -> {
                hand = message.cards.toMutableList()
            }
        }
    }
}

private fun <T> MutableList<T>.removeFirstIf(predicate: (T) -> Boolean): Boolean {
    val firstMatchingIndex = indexOfFirst(predicate)
    if (firstMatchingIndex < 0) return false
    removeAt(firstMatchingIndex)
    return true
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
