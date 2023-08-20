package testsupport.adapters

import Driver
import PlayerId
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import testsupport.ApplicationDriver
import org.openqa.selenium.chrome.ChromeDriver

class WebDriver(baseUrl: String, private val driver: ChromeDriver) : Driver {
    //private val driver = Http4kWebDriver(httpHandler(wsPort, Clock.systemDefaultZone(), false, app))

    init {
        driver.navigate().to(baseUrl)
    }

    override fun enterName(name: String) =
        driver.findElement(By.name("playerId")).sendKeys(name)

    override fun joinDefaultRoom() = driver.findElement(By.id("joinGame")).submit()

    override fun isWaitingForMorePlayers(): Boolean =
        driver.findElement(By.tagName("h2")).text.lowercase().contains("waiting for more players")

    override fun getPlayersInRoom(): List<PlayerId> =
        driver.findElement(By.id("players")).findElements(By.tagName("li")).mapNotNull { it.text }

    override fun hasGameStarted(): Boolean =
        driver.findElement(By.tagName("h2")).text.lowercase().contains("game has started")

    override fun getCardCount(name: String): Int {
        return driver.findElement(By.id("hand")).findElements(By.tagName("li")).size
    }

    override fun placeBet(bet: Int) {
        driver.findElement(By.name("bet")).sendKeys(bet.toString())
        driver.findElement(By.id("placeBet")).click()
    }

    override fun getBets(): Map<PlayerId, Int> {
        return driver.findElement(By.id("bets"))
            .findElements(By.tagName("li"))
            .associate { it.text.split(":").let { (name, bet) -> name to bet.toInt() } }
    }

    override fun startGame() {
        return driver.findElement(By.id("startGame")).submit()
    }
}
