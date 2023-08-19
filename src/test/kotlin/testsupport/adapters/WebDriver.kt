package testsupport.adapters

import App
import PlayerId
import com.tamj0rd2.webapp.httpHandler
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import testsupport.ApplicationDriver
import java.time.Clock

class WebDriver(app: App, wsPort: Int) : ApplicationDriver {
    private val driver = Http4kWebDriver(httpHandler(wsPort, Clock.systemDefaultZone(), false, app))

    init {
        driver.navigate().to("/")
    }

    override fun enterName(name: String) =
        driver.findElement(By.name("playerId")).must().sendKeys(name)

    override fun joinDefaultRoom() = driver.findElement(By.tagName("form")).must().submit()

    override fun isWaitingForMorePlayers(): Boolean =
        driver.findElement(By.tagName("h2")).must().text.lowercase().contains("waiting for more players")

    override fun getPlayersInRoom(): List<PlayerId> =
        driver.findElements(By.tagName("li")).must().mapNotNull { it.text }.apply { println(driver.findElement(By.id("players")).must().text) }

    override fun hasGameStarted(): Boolean =
        driver.findElement(By.tagName("h2")).must().text.lowercase().contains("game has started")

    private fun WebElement?.must(): WebElement = this ?: error("element not found")
    private fun List<WebElement>?.must(): List<WebElement> {
        return this ?: error("elements not found")
    }
}