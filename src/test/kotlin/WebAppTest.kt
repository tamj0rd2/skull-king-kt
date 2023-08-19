import com.tamj0rd2.webapp.httpHandler
import com.tamj0rd2.webapp.wsHandler
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.asServer
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.adapters.WebDriver
import java.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class WebAppTest : AppTestContract() {
    private val app = App()
    private val port = 9001
    private val server = PolyHandler(
        httpHandler(port, Clock.systemDefaultZone(), false, app),
        wsHandler(app)
    ).asServer(Jetty(port))

    private val chromeDrivers = mutableListOf<ChromeDriver>()
    override val makeDriver = { WebDriver(port, ChromeDriver().apply { chromeDrivers += this }) }

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