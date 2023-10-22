package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.Acknowledgements
import com.tamj0rd2.webapp.ClientMessage
import com.tamj0rd2.webapp.ServerMessage
import com.tamj0rd2.webapp.OverTheWireMessage
import com.tamj0rd2.webapp.awaitingAck
import com.tamj0rd2.webapp.overTheWireMessageLens
import com.tamj0rd2.webapp.processedMessage
import com.tamj0rd2.webapp.receivedMessage
import com.tamj0rd2.webapp.sending
import com.tamj0rd2.webapp.sentMessage
import org.http4k.client.WebsocketClient
import org.http4k.core.ContentType
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

class WebsocketDriver(private val httpClient: HttpHandler, host: String, private val ackTimeoutMs: Long = 300) :
    ApplicationDriver {
    private val httpBaseUrl = "http://$host"
    private val wsBaseUrl = "ws://$host"
    private val joinSyncObject = Object()
    private val acknowledgements = Acknowledgements(ackTimeoutMs)

    private lateinit var ws: Websocket
    private lateinit var logger: Logger
    private lateinit var playerId: PlayerId

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("$playerId:wsClient")
        this.playerId = playerId

        val req = Request(Method.POST, "$httpBaseUrl/play")
            .form("playerId", playerId.playerId)
            .header("Accept", ContentType.APPLICATION_JSON.toHeaderValue())

        val res = httpClient(req)
        when (res.status) {
            Status.OK -> {}
            Status.CONFLICT -> throw GameException.PlayerWithSameNameAlreadyJoined(playerId)
            else -> error("failed to join game - ${res.status} - ${res.bodyString()}")
        }

        ws = WebsocketClient.nonBlocking(
            Uri.of("$wsBaseUrl/$playerId"),
            onError = { throw it },
            timeout = 1.seconds.toJavaDuration(),
        )

        ws.onMessage {
            val message = overTheWireMessageLens(it)
            logger.receivedMessage(message)
            when (message) {
                is OverTheWireMessage.AcknowledgementFromServer -> {
                    message.messages.forEach(::handleMessage)
                    acknowledgements.ack(message.id)
                    logger.processedMessage(message)
                }

                is OverTheWireMessage.ProcessingFailure -> {
                    acknowledgements.nack(message.id)
                }

                is OverTheWireMessage.ToClient -> {
                    message.messages.forEach(::handleMessage)
                    logger.processedMessage(message)

                    if (message.messages.size == 1 && message.messages[0] is ServerMessage.YouJoined) {
                        synchronized(joinSyncObject) { joinSyncObject.notify() }
                        return@onMessage
                    }

                    message.acknowledge().let { response ->
                        logger.sending(response)
                        ws.send(overTheWireMessageLens(response))
                        logger.sentMessage(response)
                    }
                }

                else -> error("invalid message from server to client: $message")
            }
        }

        synchronized(joinSyncObject) { joinSyncObject.wait() }
        logger.debug("joined game")
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.BidPlaced -> {
                bids[message.playerId] = DisplayBid.Hidden
            }

            is ServerMessage.BiddingCompleted -> {
                roundPhase = RoundPhase.BiddingCompleted
                bids = message.bids.mapValues { DisplayBid.Placed(it.value.bid) }.toMutableMap()
            }

            is ServerMessage.CardPlayed -> {
                currentPlayer = message.nextPlayer
                trick.add(message.card.playedBy(message.playerId))
                if (message.playerId == playerId) {
                    hand.removeFirstIf { it.card == message.card }
                }
            }

            is ServerMessage.GameCompleted -> {
                gameState = GameState.Complete
            }

            is ServerMessage.GameStarted -> {
                playersInRoom = message.players.toMutableList()
                gameState = GameState.InProgress
            }

            is ServerMessage.PlayerJoined -> {
                playersInRoom.add(message.playerId)
                gameState =
                    if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is ServerMessage.RoundCompleted -> {
                winsOfTheRound = message.wins
            }

            is ServerMessage.RoundStarted -> {
                roundNumber = message.roundNumber
                roundPhase = RoundPhase.Bidding
                hand = message.cardsDealt.toMutableList()
                bids = playersInRoom.associateWith { DisplayBid.None }.toMutableMap()
            }

            is ServerMessage.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
                trickWinner = message.winner
            }

            is ServerMessage.TrickStarted -> {
                roundPhase = RoundPhase.TrickTaking
                trickNumber = message.trickNumber
                currentPlayer = message.firstPlayer
                trick.clear()
            }

            // TODO: get rid of this. Let the client send a JoinGame message requiring acknowledgement instead
            is ServerMessage.YouJoined -> {
                playersInRoom = message.players.toMutableList()
                gameState =
                    if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
            }

            is ServerMessage.YourTurn -> {
                hand = message.cards.toMutableList()
            }
        }
    }

    override fun bid(bid: Int): Unit {
        sendMessage(ClientMessage.Request.PlaceBid(bid))
            .onFailure { throw GameException.CannotBid("operation nacked by server") }
            .onSuccess { logger.debug("bidded $bid") }
    }

    override fun playCard(card: Card) {
        sendMessage(ClientMessage.Request.PlayCard(card.name))
            .onFailure { throw GameException.CannotPlayCard("operation nacked by server") }
            .onSuccess { logger.debug("played $card") }
    }

    private fun sendMessage(message: ClientMessage.Request): Result<Unit> {
        val otwMessage = message.overTheWire()

        return acknowledgements.waitFor(otwMessage.messageId) {
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
