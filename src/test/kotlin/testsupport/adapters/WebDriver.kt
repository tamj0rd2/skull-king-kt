package testsupport.adapters

import Card
import GamePhase
import GameState
import Hand
import PlayerId
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ApplicationDriver

class WebDriver(baseUrl: String, private val driver: ChromeDriver) : ApplicationDriver {
    init {
        driver.navigate().to(baseUrl)
    }

    override fun enterName(name: String) = driver.findElement(By.name("playerId")).sendKeys(name)

    override fun joinDefaultRoom() = driver.findElement(By.id("joinGame")).submit()

    override val playersInRoom: List<PlayerId>
        get() = driver.findElement(By.id("players")).findElements(By.tagName("li")).mapNotNull { it.text }

    override val hand: Hand
        get() = driver.findElement(By.id("hand")).findElements(By.tagName("li")).map { Card() }

    override fun placeBet(bet: Int) {
        driver.findElement(By.name("bet")).sendKeys(bet.toString())
        driver.findElement(By.id("placeBet")).click()
    }

    override val gameState: GameState get() {
        val gameState = driver.findElement(By.id("gameState")).text.lowercase()
        if (gameState.contains("waiting for more players")) return GameState.WaitingForMorePlayers
        if (gameState.contains("game has started")) return GameState.InProgress
        return GameState.WaitingToStart
    }

    override val gamePhase: GamePhase get() {
        val gamePhase = driver.findElement(By.id("gamePhase")).text.lowercase()
        if (gamePhase.contains("place your bid")) return GamePhase.Bidding
        if (gamePhase.contains("it's trick taking time")) return GamePhase.TrickTaking
        TODO("got an unknown game phase: $gamePhase")
    }

    override val bets: Map<PlayerId, Int>
        get() = driver.findElement(By.id("bets"))
            .findElements(By.tagName("li"))
            .associate { it.text.split(":").let { (name, bet) -> name to bet.toInt() } }

    override val playersWhoHavePlacedBets: List<PlayerId>
        get() = driver.findElement(By.id("playersWhoHaveBet"))
            .findElements(By.tagName("li"))
            .map { it.text }
}
