import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import testsupport.ParticipateInGames
import testsupport.ManageGames
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

// NOTE: this needs to manually be kept in line with the version in gradle.build.kts
const val chromeVersion = 114

class WebAppTest : AppTestContract() {
    private val port = 9001
    private val server = WebServer.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()

    private val chromeOptions get() = ChromeOptions()
        .addArguments("--headless=chrome")
        .setBinary("${System.getProperty("user.dir")}/.chrome/chrome-$chromeVersion.app/Contents/MacOS/Google Chrome for Testing")

    override val participateInGames = { ParticipateInGames(newWebDriver()) }
    override val manageGames = { ManageGames(HTTPDriver(baseUrl, httpClient)) }

    private fun newWebDriver(): WebDriver {
        System.setProperty("webdriver.chrome.driver", "${System.getProperty("user.dir")}/.chromedriver/chromedriver-$chromeVersion");
        return WebDriver(baseUrl, ChromeDriver(chromeOptions).apply { chromeDrivers += this })
    }
    private val chromeDrivers = mutableListOf<ChromeDriver>()

    @BeforeTest
    fun before() {
        httpClient.start()
        server.start()
    }

    @AfterTest
    fun after() {
        chromeDrivers.forEach { it.quit() }
        httpClient.stop()
        server.stop()
    }
}