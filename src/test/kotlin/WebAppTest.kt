import org.openqa.selenium.chrome.ChromeDriver
import testsupport.adapters.WebDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class WebAppTest : AppTestContract() {
    private val port = 9001
    private val server = WebServer.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"

    override val makeDriver = { WebDriver(baseUrl, ChromeDriver().apply { chromeDrivers += this }) }
    private val chromeDrivers = mutableListOf<ChromeDriver>()

    @BeforeTest
    fun before() {
        server.start()
    }

    @AfterTest
    fun after() {
        chromeDrivers.forEach { it.quit() }
        server.stop()
    }
}