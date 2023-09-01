import org.eclipse.jetty.client.HttpClient
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.SkipWip
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebDriver
import java.net.ServerSocket

// this needs to manually be kept in line with the version in gradle.build.kts
private const val chromeVersion = 114
private const val headless = true

private val chromeDriverBinary = "${System.getProperty("user.dir")}/.chromedriver/chromedriver-$chromeVersion"
private val chromeOptions = ChromeOptions()
    .apply { if (headless) addArguments("--headless") }
    .setBinary("${System.getProperty("user.dir")}/.chrome/chrome-$chromeVersion.app/Contents/MacOS/Google Chrome for Testing")

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
        System.setProperty("webdriver.chrome.driver", chromeDriverBinary)
    }

    override fun setup() {
        server.start()
        httpClient.start()
    }

    override fun teardown() {
        chromeDrivers.forEach {
            it.quit()
        }
        httpClient.stop()
        server.stop()
    }

    override fun participateInGames(): ParticipateInGames {
        val chromeDriver = ChromeDriver(chromeOptions).apply {
            this.navigate().to(baseUrl)
            chromeDrivers += this
        }
        return ParticipateInGames(WebDriver(chromeDriver))
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
})
