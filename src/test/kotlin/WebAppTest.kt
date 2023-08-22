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

class WebAppTest : AppTestContract() {
    private val port = 9001
    private val server = WebServer.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()
    private val chromeOptions get() = ChromeOptions().addArguments("--headless=chrome")

    override val participateInGames = { ParticipateInGames(newWebDriver()) }
    override val manageGames = { ManageGames(HTTPDriver(baseUrl, httpClient)) }

    private fun newWebDriver() = WebDriver(baseUrl, ChromeDriver(chromeOptions).apply { chromeDrivers += this })
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