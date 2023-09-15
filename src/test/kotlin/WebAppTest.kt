
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
import testsupport.SkipWip
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebDriver
import java.net.ServerSocket
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

    val chromeDriverBinary = "${System.getProperty("user.dir")}/.chromedriver/chromedriver-$CHROME_VERSION"
    val chromeOptions: ChromeOptions = ChromeOptions()
        .setBinary("${System.getProperty("user.dir")}/.chrome/chrome-$CHROME_VERSION.app/Contents/MacOS/Google Chrome for Testing")
        .apply {
            if (HEADLESS) addArguments("--headless")
            setCapability("goog:loggingPrefs", loggingPreferences)
            setCapability("goog:chromeOptions", mapOf("perfLoggingPrefs" to loggingPreferences))
        }
}

@SkipWip
@Execution(ExecutionMode.CONCURRENT)
class WebAppTest : AppTestContract(object : TestConfiguration {
    private val port = ServerSocket(0).run {
        close()
        localPort
    }
    private val server = WebServer.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()
    private val chromeDrivers = mutableListOf<ChromeDriver>()

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

    override fun participateInGames(): ParticipateInGames {
        val chromeDriver = ChromeDriver(Config.chromeOptions).apply {
            devTools.createSession()
            devTools.domains.events().addJavascriptExceptionListener { j: JavascriptException ->
                println("JAVASCRIPT ERROR!: ${j.localizedMessage}")
            }
            devTools.domains.events().addConsoleListener { println(it.messages.joinToString(" ")) }

            this.navigate().to(baseUrl)
            chromeDrivers += this
        }
        return ParticipateInGames(WebDriver(chromeDriver))
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
})
