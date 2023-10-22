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
import com.tamj0rd2.webapp.sentAckFor
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

                    ws.send(overTheWireMessageLens(message.acknowledge()))
                    logger.sentAckFor(message)
                }
                else -> error("invalid message from server to client: $message")
            }
        }

        synchronized(joinSyncObject) { joinSyncObject.wait() }
        logger.info("joined game")
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
                if (message.playerId == playerId) hand = hand.filter { it.card != message.card }.toMutableList()
            }

            MessageToClient.GameCompleted -> TODO()
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
                hand = message.cardsDealt
            }

            is MessageToClient.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
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
                hand = message.cards
            }
        }
    }

    override fun bid(bid: Int) {
        sendMessage(MessageFromClient.BidPlaced(bid))
        logger.info("bidded $bid")
    }

    override fun playCard(card: Card) {
        sendMessage(MessageFromClient.CardPlayed(card.name))
        logger.info("played $card")
    }

    private fun sendMessage(message: MessageFromClient) {
        val otwMessage = message.overTheWire()
        logger.sending(otwMessage)
        ws.send(overTheWireMessageLens(otwMessage))

        logger.awaitingAck(otwMessage)
        ack.waitFor(otwMessage.messageId)
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
    override var hand = listOf<CardWithPlayability>()
    override var bids = mutableMapOf<PlayerId, DisplayBid>()
}
