import org.eclipse.jetty.client.HttpClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.BrowserDriver
import testsupport.adapters.HTTPDriver
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests
import testsupport.logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Disabled
@SkipWipTests
@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
class DeployedBrowserAppTest : AppTestContract(DeployedTestConfiguration())

class DeployedTestConfiguration : TestConfiguration {
    // TODO: I shouldn't really need this
    override val automaticGameMasterDelay: Duration = 0.seconds

    // TODO: make this configurable
    private val baseUrl by lazy { "http://localhost:8080" }
    private val httpClient by lazy { HttpClient() }
    private val chromeDrivers by lazy { mutableListOf<ChromeDriver>() }

    init {
        System.setProperty("webdriver.chrome.driver", Config.chromeDriverBinary)
    }

    override fun setup() {
        httpClient.start()
    }

    override fun teardown() {
        if (!Config.HEADLESS) Thread.sleep(5.seconds.inWholeMilliseconds)
        chromeDrivers.forEach {
            it.quit()
        }
        httpClient.stop()
    }

    override fun automateGameMasterCommands() {
        TODO("Not yet implemented")
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

