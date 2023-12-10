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
import kotlin.time.Duration.Companion.milliseconds

class WebAppTestConfiguration : TestConfiguration {
    private val port by lazy {
        ServerSocket(0).run {
            close()
            localPort
        }
    }
    override var automaticGameMasterDelay: Duration = 200.milliseconds
    private var automateGameMasterCommands = false

    private val acknowledgementTimeoutMs = 100L
    private val server by lazy {
        Server.make(
            port,
            devServer = false,
            automateGameMasterCommands = automateGameMasterCommands,
            automaticGameMasterDelayOverride = automaticGameMasterDelay,
            acknowledgementTimeoutMs = acknowledgementTimeoutMs,
            gracefulShutdownTimeout = Duration.ZERO,
        )
    }
    private val host by lazy { "localhost:$port" }
    private val baseUrl by lazy { "http://$host" }
    private val httpClient = HttpClient()

    override fun setup() {
        server.start()
        httpClient.start()
    }

    override fun teardown() {
        httpClient.stop()
        server.stop()
    }

    override fun automateGameMasterCommands() {
        automateGameMasterCommands = true
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

@Execution(ExecutionMode.SAME_THREAD)
class WebAppTest : AppTestContract(WebAppTestConfiguration())
