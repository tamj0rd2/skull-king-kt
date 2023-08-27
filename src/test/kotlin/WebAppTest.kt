import org.eclipse.jetty.client.HttpClient
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import testsupport.ParticipateInGames
import testsupport.ManageGames
import testsupport.SkipWip
import testsupport.adapters.HTTPDriver
import testsupport.adapters.WebDriver

// NOTE: this needs to manually be kept in line with the version in gradle.build.kts
private const val chromeVersion = 114
private const val headless = true

private val chromeDriverBinary = "${System.getProperty("user.dir")}/.chromedriver/chromedriver-$chromeVersion"
private val chromeOptions = ChromeOptions()
    .apply { if (headless) addArguments("--headless") }
    .setBinary("${System.getProperty("user.dir")}/.chrome/chrome-$chromeVersion.app/Contents/MacOS/Google Chrome for Testing")

@SkipWip
class WebAppTest : AppTestContract(object : TestConfiguration {
    private val port = 9001
    private val server = WebServer.make(port, hotReload = false)
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()
    private val chromeDrivers = mutableListOf<ChromeDriver>()

    init {
        System.setProperty("webdriver.chrome.driver", chromeDriverBinary);
    }

    override fun setup() {
        server.start()
        httpClient.start()
        chromeDrivers.forEach { it.navigate().to(baseUrl) }
    }

    override fun teardown() {
        chromeDrivers.forEach {
            //it.findElement(By.tagName("body")).getAttribute("outerHTML").let(::println)
            it.quit()
        }
        httpClient.stop()
        server.stop()
    }

    override fun participateInGames(): ParticipateInGames {
        val chromeDriver = ChromeDriver(chromeOptions).apply { chromeDrivers += this }
        return ParticipateInGames(WebDriver(chromeDriver))
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
})
