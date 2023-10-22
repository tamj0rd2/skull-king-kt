package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.Acknowledgements
import com.tamj0rd2.webapp.MessageFromClient
import com.tamj0rd2.webapp.MessageToClient
import com.tamj0rd2.webapp.OverTheWireMessage
import com.tamj0rd2.webapp.awaitingAck
import com.tamj0rd2.webapp.overTheWireMessageLens
import com.tamj0rd2.webapp.processedMessage
import com.tamj0rd2.webapp.receivedMessage
import com.tamj0rd2.webapp.sending
import com.tamj0rd2.webapp.sentAck
import org.http4k.client.WebsocketClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.websocket.Websocket
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testsupport.ApplicationDriver
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class WebsocketDriver(private val httpClient: HttpHandler, host: String) : ApplicationDriver {
    private val httpBaseUrl = "http://$host"
    private val wsBaseUrl = "ws://$host"
    private val joinSyncObject = Object()
    private val ack = Acknowledgements()

    private lateinit var ws: Websocket
    private lateinit var logger: Logger
    private lateinit var playerId: PlayerId

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("$playerId:wsClient")
        this.playerId = playerId

        val res = httpClient(Request(Method.POST, "$httpBaseUrl/play").form("playerId", playerId.playerId))
        if (res.status != Status.OK) error("failed to join game - ${res.status} - ${res.bodyString()}")

        ws = WebsocketClient.nonBlocking(
            Uri.of("$wsBaseUrl/$playerId"),
            onError = { throw it },
            timeout = 1.seconds.toJavaDuration(),
        )

        ws.onMessage {
            val message = overTheWireMessageLens(it)
            logger.receivedMessage(message)
            when(message) {
                is OverTheWireMessage.AcknowledgementFromServer -> {
                    message.messages.forEach(::handleMessage)
                    ack(message.id)
                    logger.processedMessage(message)
                }
                is OverTheWireMessage.MessagesToClient -> {
                    message.messages.forEach(::handleMessage)
                    logger.processedMessage(message)

                    if (message.messages.size == 1 && message.messages[0] is MessageToClient.YouJoined) {
                        synchronized(joinSyncObject) { joinSyncObject.notify() }
                        return@onMessage
                    }

                    message.acknowledge().let { response ->
                        logger.sending(response)
                        ws.send(overTheWireMessageLens(response))
                        logger.sentAck(response)
                    }
                }
                else -> error("invalid message from server to client: $message")
            }
        }

        synchronized(joinSyncObject) { joinSyncObject.wait() }
        logger.warn("joined game")
    }

    private fun handleMessage(message: MessageToClient) {
        when (message) {
            is MessageToClient.BidPlaced -> {
            }

            is MessageToClient.BiddingCompleted -> {
                roundPhase = RoundPhase.BiddingCompleted
                bids = message.bids.mapValues { DisplayBid.Placed(it.value.bid) }.toMutableMap()
            }

            is MessageToClient.CardPlayed -> {
                trick.add(message.card.playedBy(message.playerId))
                if (message.playerId == playerId) {
                    hand.removeFirstIf { it.card == message.card }
                }
            }

            is MessageToClient.GameCompleted -> {
                gameState = GameState.Complete
            }
            is MessageToClient.GameStarted -> {
                playersInRoom = message.players.toMutableList()
                gameState = GameState.InProgress
            }

            is MessageToClient.PlayerJoined -> {
                playersInRoom.add(message.playerId)
                gameState =
                    if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is MessageToClient.RoundCompleted -> {
                winsOfTheRound = message.wins
            }

            is MessageToClient.RoundStarted -> {
                roundNumber = message.roundNumber
                roundPhase = RoundPhase.Bidding
                hand = message.cardsDealt.toMutableList()
            }

            is MessageToClient.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
                trickWinner = message.winner
            }

            is MessageToClient.TrickStarted -> {
                roundPhase = RoundPhase.TrickTaking
                trickNumber = message.trickNumber
                currentPlayer = message.firstPlayer
                trick.clear()
            }

                // TODO: get rid of this. Let the client send a JoinGame message requiring acknowledgement instead
            is MessageToClient.YouJoined -> {
                playersInRoom = message.players.toMutableList()
                gameState = if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is MessageToClient.YourTurn -> {
                hand = message.cards.toMutableList()
            }
        }
    }

    override fun bid(bid: Int) {
        sendMessage(MessageFromClient.BidPlaced(bid))
        logger.warn("bidded $bid")
    }

    override fun playCard(card: Card) {
        sendMessage(MessageFromClient.CardPlayed(card.name))
        logger.warn("played $card")
    }

    private fun sendMessage(message: MessageFromClient) {
        val otwMessage = message.overTheWire()
        ack.waitFor(otwMessage.messageId) {
            logger.sending(otwMessage)
            ws.send(overTheWireMessageLens(otwMessage))
            logger.awaitingAck(otwMessage)
        }
    }

    override var winsOfTheRound: Map<PlayerId, Int> = emptyMap()

    override var trickWinner: PlayerId? = null
    override var currentPlayer: PlayerId? = null
    override var trickNumber: Int? = null
    override var roundNumber: Int? = null
    override val trick = mutableListOf<PlayedCard>()
    override var roundPhase: RoundPhase? = null

    override var gameState: GameState? = null
    override var playersInRoom = mutableListOf<PlayerId>()
    override var hand = mutableListOf<CardWithPlayability>()
    override var bids = mutableMapOf<PlayerId, DisplayBid>()
}

private fun <T> MutableList<T>.removeFirstIf(predicate: (T) -> Boolean): Boolean {
    val firstMatchingIndex = indexOfFirst(predicate)
    if (firstMatchingIndex < 0) return false
    removeAt(firstMatchingIndex)
    return true
}
