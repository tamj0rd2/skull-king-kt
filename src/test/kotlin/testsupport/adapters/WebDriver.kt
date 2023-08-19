package testsupport.adapters

import PlayerId
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import testsupport.ApplicationDriver
import org.openqa.selenium.chrome.ChromeDriver

class WebDriver(port: Int, private val driver: ChromeDriver) : ApplicationDriver {
    //private val driver = Http4kWebDriver(httpHandler(wsPort, Clock.systemDefaultZone(), false, app))

    init {
        driver.navigate().to("http://localhost:$port")
    }

    override fun enterName(name: String) =
        driver.findElement(By.name("playerId")).must().sendKeys(name)

    override fun joinDefaultRoom() = driver.findElement(By.tagName("form")).must().submit()

    override fun isWaitingForMorePlayers(): Boolean =
        driver.findElement(By.tagName("h2")).must().text.lowercase().contains("waiting for more players")

    override fun getPlayersInRoom(): List<PlayerId> =
        driver.findElements(By.tagName("li")).must().mapNotNull { it.text }

    override fun hasGameStarted(): Boolean =
        driver.findElement(By.tagName("h2")).must().text.lowercase().contains("game has started")

    private fun WebElement?.must(): WebElement = this ?: error("element not found")
    private fun List<WebElement>?.must(): List<WebElement> {
        return this ?: error("elements not found")
    }
}