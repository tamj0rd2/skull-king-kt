
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebsocketDriver
import java.net.ServerSocket

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
