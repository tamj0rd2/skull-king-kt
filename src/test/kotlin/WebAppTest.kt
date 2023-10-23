
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebsocketDriver
import java.net.ServerSocket
import kotlin.time.Duration

@Execution(ExecutionMode.SAME_THREAD)
class WebAppTest : AppTestContract(WebAppTestConfiguration())

class WebAppTestConfiguration : TestConfiguration {
    private val port = ServerSocket(0).run {
        close()
        localPort
    }
    private val acknowledgementTimeoutMs = 100L
    private val server = Server.make(
        port,
        hotReload = false,
        automateGameMasterCommands = false,
        acknowledgementTimeoutMs = acknowledgementTimeoutMs,
        gracefulShutdownTimeout = Duration.ZERO
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
                host = host,
                ackTimeoutMs = acknowledgementTimeoutMs
            )
        )
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
}
