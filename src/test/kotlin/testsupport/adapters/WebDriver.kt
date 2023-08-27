package testsupport.adapters

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GamePhase
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.Trick
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ApplicationDriver

class WebDriver(private val driver: ChromeDriver) : ApplicationDriver {
    override fun enterPlayerId(playerId: String) = driver.findElement(By.name("playerId")).sendKeys(playerId)

    override fun joinDefaultRoom() = driver.findElement(By.id("joinGame")).submit()

    override fun placeBet(bet: Int) {
        driver.findElement(By.name("bet")).sendKeys(bet.toString())
        driver.findElement(By.id("placeBet")).click()
    }

    override fun playCard(playerId: String, cardId: CardId) {
        driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .find { it.toCardId() == cardId }
            .let { it ?: throw GameException.CardNotInHand(playerId, cardId, TODO()) }
            .findElement(By.tagName("button"))
            .click()
    }

    override val trickNumber: Int
        get() = TODO("Not yet implemented")

    override val roundNumber: Int
        get() = TODO("Not yet implemented")

    override val playersInRoom: List<PlayerId>
        get() = driver.findElement(By.id("players"))
            .findElements(By.tagName("li"))
            .mapNotNull { it.text }

    override val hand: Hand
        get() = driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .map { Card(it.toCardId()) }

    override val trick: Trick
        get() = driver.findElement(By.id("trick"))
            .findElements(By.tagName("li"))
            .map {
                it.text.split(":").let { (name, cardId) -> PlayedCard(name, Card(cardId)) }
            }

    override val gameState: GameState
        get() {
            val gameState = driver.findElement(By.id("gameState")).text.lowercase()
            if (gameState.contains("waiting for more players")) return GameState.WaitingForMorePlayers
            if (gameState.contains("game has started")) return GameState.InProgress
            return GameState.WaitingToStart
        }

    override val gamePhase: GamePhase
        get() {
            val gamePhase = driver.findElement(By.id("gamePhase")).text.lowercase()
            if (gamePhase.contains("place your bid")) return GamePhase.Bidding
            if (gamePhase.contains("it's trick taking time")) return GamePhase.TrickTaking
            // TODO: this could use its own error code too
            TODO("got an unknown game phase: $gamePhase")
        }

    override val bets: Map<PlayerId, Bid>
        get() = driver.findElement(By.id("bets"))
            .findElements(By.tagName("li"))
            .associate {
                val (name, bet) = it.text.split(":").apply { if (size < 2) return@associate this[0] to Bid.None }
                if (bet == "has bet") return@associate name to Bid.IsHidden
                name to Bid.Placed(bet.toInt())
            }
}

private fun WebElement.toCardId(): CardId = text.removeSuffix("Play")
