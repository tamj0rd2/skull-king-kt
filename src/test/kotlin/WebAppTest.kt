
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.Websocket
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WsDriver
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
        wsClients.forEach { it.close() }
    }

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(WsDriver(::newWs))

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))

    private fun newWs(playerId: PlayerId) = WebsocketClient.nonBlocking(Uri.run { of("ws://localhost:$port/${playerId}") }).also { wsClients.add(it) }
})


