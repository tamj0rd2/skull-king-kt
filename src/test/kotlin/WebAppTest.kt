
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.CustomJackson.auto
import com.tamj0rd2.webapp.MessageToClient
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testsupport.ApplicationDriver
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.HTTPDriver
import java.net.ServerSocket

class WebAppTest : AppTestContract(object : TestConfiguration {
    private val port = ServerSocket(0).apply { close() }.localPort
    private val server = Server.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()
    private val wsClients = mutableListOf<Websocket>()

    override fun setup() {
        server.start()
    }

    override fun teardown() {
        server.close()
    }

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(object : ApplicationDriver {
        private val _playersInRoom = mutableListOf<PlayerId>()
        private val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
        private lateinit var logger: Logger

        private lateinit var playerId: PlayerId

        override fun joinGame(playerId: PlayerId) {
            logger = LoggerFactory.getLogger("$playerId ws client")

            _playersInRoom.add(playerId)
            this.playerId = playerId

            // I need to call the play endpoint first. duh
            val ws = WebsocketClient.nonBlocking(Uri.run { of("ws://localhost:$port/${playerId}") }).also { wsClients.add(it) }
            ws.onMessage {
                logger.info("received message: ${messageToClientLens(it)}")
                println("You'll never see this :(")
            }

            //val ws = WebsocketClient.blocking(Uri.of("ws://localhost:$port/${playerId.playerId.urlEncoded()}"))
            //ws.received().toList().forEach(::println)
            //val scope = CoroutineScope(Dispatchers.Main)
            //scope.launch {
            //
            //    println("waiting for messages...")
            //}
        }

        override fun bid(bid: Int) {
            TODO("Not yet implemented")
        }

        override fun playCard(card: Card) {
            TODO("Not yet implemented")
        }

        override fun isCardPlayable(card: Card): Boolean {
            TODO("Not yet implemented")
        }

        override val winsOfTheRound: Map<PlayerId, Int>
            get() = TODO("Not yet implemented")

        override val trickWinner: PlayerId?
            get() = TODO("Not yet implemented")

        override val currentPlayer: PlayerId?
            get() = TODO("Not yet implemented")

        override val trickNumber: Int?
            get() = TODO("Not yet implemented")

        override val roundNumber: Int?
            get() = TODO("Not yet implemented")

        override val trick: List<PlayedCard>
            get() = TODO("Not yet implemented")

        override val roundPhase: RoundPhase?
            get() = TODO("Not yet implemented")

        override val gameState: GameState?
            get() = TODO("Not yet implemented")

        override val playersInRoom: List<PlayerId>
            get() = _playersInRoom

        override val hand: Hand
            get() = TODO("Not yet implemented")

        override val bids: Map<PlayerId, DisplayBid>
            get() = TODO("Not yet implemented")
    })

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
})
