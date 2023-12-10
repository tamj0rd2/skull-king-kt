
import com.tamj0rd2.webapp.Frontend
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.BrowserDriver
import testsupport.adapters.HTTPDriver
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.logger
import java.net.ServerSocket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private object Config {
    // this needs to manually be kept in line with the version in gradle.build.kts
    const val CHROME_VERSION = 114
    const val HEADLESS = true

    val loggingPreferences = LoggingPreferences().apply {
        enable(LogType.PERFORMANCE, java.util.logging.Level.ALL)
        enable(LogType.BROWSER, java.util.logging.Level.ALL)
        enable(LogType.CLIENT, java.util.logging.Level.ALL)
    }

    // TODO: use a docker chrome driver instead of installing it locally. It's so much easier.
    val chromeDriverBinary = "${System.getProperty("user.dir")}/../.chromedriver/chromedriver-$CHROME_VERSION"
    val chromeOptions: ChromeOptions = ChromeOptions()
        .setBinary("${System.getProperty("user.dir")}/../.chrome/chrome-$CHROME_VERSION.app/Contents/MacOS/Google Chrome for Testing")
        .apply {
            if (HEADLESS) addArguments("--headless")
            setCapability("goog:loggingPrefs", loggingPreferences)
            setCapability("goog:chromeOptions", mapOf("perfLoggingPrefs" to loggingPreferences))
        }
}

class BrowserTestConfiguration(frontend: Frontend) : TestConfiguration {
    private val port by lazy {
        ServerSocket(0).run {
            close()
            localPort
        }
    }
    override var automaticGameMasterDelay: Duration = 1.seconds
    private var automateGameMasterCommands = false

    private val server by lazy {
        Server.make(
            port,
            devServer = false,
            automateGameMasterCommands = automateGameMasterCommands,
            automaticGameMasterDelayOverride = automaticGameMasterDelay,
            acknowledgementTimeoutMs = 2000,
            frontend = frontend
        )
    }
    private val baseUrl by lazy { "http://localhost:$port" }
    private val httpClient by lazy { HttpClient() }
    private val chromeDrivers by lazy { mutableListOf<ChromeDriver>() }

    init {
        System.setProperty("webdriver.chrome.driver", Config.chromeDriverBinary)
    }

    override fun setup() {
        server.start()
        httpClient.start()
    }

    override fun teardown() {
        if (!Config.HEADLESS) Thread.sleep(5.seconds.inWholeMilliseconds)
        chromeDrivers.forEach {
            it.quit()
        }
        httpClient.stop()
        server.stop()
    }

    override fun automateGameMasterCommands() {
        automateGameMasterCommands = true
    }

    override fun participateInGames(): ParticipateInGames {
        val chromeDriver = ChromeDriver(Config.chromeOptions).apply {
            devTools.createSession()
            devTools.domains.events().addJavascriptExceptionListener { j: JavascriptException ->
                logger.error("JS error", j)
            }
            devTools.domains.events().addConsoleListener { println(it.messages.joinToString(" ")) }

            this.navigate().to(baseUrl)
            chromeDrivers += this
        }
        return ParticipateInGames(BrowserDriver(chromeDriver))
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
}

@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
class BrowserAppTest : AppTestContract(BrowserTestConfiguration(frontend = Frontend.WebComponents))
