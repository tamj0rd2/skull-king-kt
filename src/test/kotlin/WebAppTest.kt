
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.MessageFromClient
import com.tamj0rd2.webapp.MessageToClient
import com.tamj0rd2.webapp.Server
import com.tamj0rd2.webapp.messageToClientLens
import com.tamj0rd2.webapp.messageToServerLens
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
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
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.HTTPDriver
import java.net.ServerSocket
import java.time.Instant.now
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class WebAppTest : AppTestContract(WebAppTestConfiguration())

class WebAppTestConfiguration : TestConfiguration {
    private val port = ServerSocket(0).run {
        close()
        localPort
    }
    private val server = Server.make(
        port,
        hotReload = false,
        automateGameMasterCommands = false,
    )
    private val host = "localhost:$port"
    private val baseUrl = "http://$host"
    private val httpClient = HttpClient()

    override fun setup() {
        server.start()
        httpClient.start()
    }

    override fun teardown() {
        httpClient.stop()
        server.stop()
    }

    override fun participateInGames(): ParticipateInGames {
        return ParticipateInGames(
            WebsocketDriver(
                JettyClient.invoke(httpClient),
                host
            )
        )
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
}

class WebsocketDriver(private val httpClient: HttpHandler, host: String) : ApplicationDriver {
    private val httpBaseUrl = "http://$host"
    private val wsBaseUrl = "ws://$host"
    private val syncObject = Object()

    private lateinit var ws: Websocket
    private lateinit var logger: Logger
    private lateinit var playerId: PlayerId

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("$playerId:wsClient")
        this.playerId = playerId

        val res = httpClient(Request(Method.POST, "$httpBaseUrl/play").form("playerId", playerId.playerId))
        if (res.status != Status.OK) {
            error("failed to join game")
        }

        ws = WebsocketClient.nonBlocking(
            Uri.of("$wsBaseUrl/$playerId"),
            onError = { throw it },
            timeout = 1.seconds.toJavaDuration(),
        )

        ws.onMessage {
            val message = messageToClientLens(it)
            logger.info("received message: $message")
            handleMessage(message)
            logger.info("processed message: $message")
        }

        synchronized(syncObject) { syncObject.wait() }
    }

    private fun handleMessage(message: MessageToClient) {
        when (message) {
            is MessageToClient.Acknowledgement -> synchronized(syncObject) {
                acknowledgements.add(message.messageId)
                syncObject.notify()
            }

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

            is MessageToClient.Multi -> message.messages.forEach(::handleMessage)
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

            is MessageToClient.YouJoined -> synchronized(syncObject) {
                playersInRoom = message.players.toMutableList()
                gameState = if (message.waitingForMorePlayers) GameState.WaitingForMorePlayers else GameState.WaitingToStart
                // TODO: get rid of this. Let the client send a JoinGame message requiring acknowledgement instead
                syncObject.notify()
            }

            is MessageToClient.YourTurn -> {
                hand = message.cards
            }
        }

        if (message is MessageToClient.MessageRequiringAcknowledgement) {
            sendMessage(message.acknowledge())
        }
    }

    override fun bid(bid: Int) {
        sendMessage(MessageFromClient.BidPlaced(bid))
        logger.info("bidded")
    }

    override fun playCard(card: Card) {
        sendMessage(MessageFromClient.CardPlayed(card.name))
        logger.info("played card")
    }

    private val acknowledgements = mutableSetOf<UUID>()

    private fun sendMessage(message: MessageFromClient) {
        synchronized(syncObject) {
            logger.info("sending $message")
            ws.send(messageToServerLens(message))

            if (message !is MessageFromClient.MessageRequiringAcknowledgement) return

            logger.info("waiting for acknowledgement for ${message.messageId} $message")
            val mustEndBy = now().plusSeconds(1)
            val messageHasBeenAcknowledged = { acknowledgements.contains(message.messageId) }

            do { syncObject.wait(100) } while (now() < mustEndBy && !messageHasBeenAcknowledged())
            if (messageHasBeenAcknowledged())
                acknowledgements.remove(message.messageId)
            else
                error("$message ${message.messageId} from $playerId was not acknowledged within the timeout")
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
    override var hand = listOf<CardWithPlayability>()
    override var bids = mutableMapOf<PlayerId, DisplayBid>()
}
