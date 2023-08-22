package testsupport.adapters

import Driver
import GameState
import PlayerId
import com.natpryce.hamkrest.contains
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver

class WebDriver(baseUrl: String, private val driver: ChromeDriver) : Driver {
    init {
        driver.navigate().to(baseUrl)
    }

    override fun enterName(name: String) = driver.findElement(By.name("playerId")).sendKeys(name)

    override fun joinDefaultRoom() = driver.findElement(By.id("joinGame")).submit()

    override val playersInRoom: List<PlayerId>
        get() = driver.findElement(By.id("players")).findElements(By.tagName("li")).mapNotNull { it.text }

    override val cardCount: Int
        get() = driver.findElement(By.id("hand")).findElements(By.tagName("li")).size

    override fun placeBet(bet: Int) {
        driver.findElement(By.name("bet")).sendKeys(bet.toString())
        driver.findElement(By.id("placeBet")).click()
    }

    override val gameState: GameState get() {
        val gameState = driver.findElement(By.tagName("h2")).text.lowercase()
        if (gameState.contains("waiting for more players")) return GameState.WaitingForMorePlayers
        if (gameState.contains("game has started")) return GameState.InProgress
        return GameState.WaitingToStart
    }

    override val bets: Map<PlayerId, Int>
        get() = driver.findElement(By.id("bets"))
            .findElements(By.tagName("li"))
            .associate { it.text.split(":").let { (name, bet) -> name to bet.toInt() } }

    override val playersWhoHavePlacedBets: List<PlayerId>
        get() = driver.findElement(By.id("playersWhoHaveBet"))
            .findElements(By.tagName("li"))
            .map { it.text }

    // TODO: this doesn't need to be in a web driver. I can just use an HTTP client to make the requests
    // This is breaking cohesion
    override fun startGame() {
        return driver.findElement(By.id("startGame")).submit()
    }

    override fun startTrickTaking() = driver.findElement(By.id("startTrickTaking")).submit()
}
