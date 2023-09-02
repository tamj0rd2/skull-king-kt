package testsupport.adapters

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.RoundPhase
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
    private lateinit var playerId: String

    override fun joinGame(playerId: PlayerId) {
        driver.findElement(By.name("playerId")).sendKeys(playerId)
        this.playerId = playerId

        driver.findElement(By.id("joinGame")).submit().apply {
            val errorElements = driver.findElements(By.id("errorMessage"))
            if (errorElements.isNotEmpty()) {
                when (val errorMessage = errorElements.single().text) {
                    GameException.PlayerWithSameNameAlreadyJoined::class.simpleName!! -> throw GameException.PlayerWithSameNameAlreadyJoined(
                        playerId
                    )

                    else -> error("unknown error message: $errorMessage")
                }
            }
        }
    }

    override fun placeBet(bet: Int) {
        driver.findElement(By.name("bet")).sendKeys(bet.toString())
        driver.findElement(By.id("placeBet")).click()
    }

    override fun playCard(cardId: CardId) {
        driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .find { it.toCardId() == cardId }
            .let { it ?: throw GameException.CardNotInHand(playerId, cardId) }
            .findElement(By.tagName("button"))
            .click()
    }

    override val biddingError: GameErrorCode?
        get() = debugException {
            driver.findElement(By.id("biddingError"))
                .getAttribute("data-errorCode")
                .let { if (it.isNullOrEmpty()) null else GameErrorCode.from(it) }
        }

    override val trickNumber: Int get() = debugException {
        driver.findElement(By.id("trickNumber")).text.toInt()
    }

    override val roundNumber: Int get() = debugException {
        driver.findElement(By.id("roundNumber")).text.toInt()
    }

    override val playersInRoom: List<PlayerId>
        get() = debugException {
            driver.findElement(By.id("players"))
                .findElements(By.tagName("li"))
                .mapNotNull { it.text }
        }

    override val hand: Hand
        get() = debugException {
            driver.findElement(By.id("hand"))
                .findElements(By.tagName("li"))
                .map { Card(it.toCardId()) }
        }

    override val trick: Trick
        get() = debugException {
            driver.findElement(By.id("trick"))
                .findElements(By.tagName("li"))
                .mapNotNull {
                    it.text
                        .apply { if (isEmpty()) return@mapNotNull null }
                        .split(":")
                        .apply { if (size < 2) error("cannot parse trick list item: ${it.getAttribute("outerHTML")}") }
                        .let { (name, cardId) -> PlayedCard(name, Card(cardId)) }
                }
        }

    override val gameState: GameState
        get() = debugException {
            val gameState = driver.findElement(By.id("gameState")).text.lowercase()
            when {
                gameState.contains("waiting for more players") -> GameState.WaitingForMorePlayers
                gameState.contains("game has started") -> GameState.InProgress
                gameState.contains("the game is over") -> GameState.Complete
                else -> GameState.WaitingToStart
            }
        }

    override val roundPhase: RoundPhase
        get() = debugException {
            val gamePhase = driver.findElement(By.id("gamePhase")).text.lowercase()
            when {
                gamePhase.contains("place your bid") -> RoundPhase.Bidding
                gamePhase.contains("it's trick taking time") -> RoundPhase.TrickTaking
                gamePhase.contains("trick complete") -> RoundPhase.TrickComplete
                else -> error("could not parse game phase from: '$gamePhase'")
            }
        }

    override val bets: Map<PlayerId, Bid>
        get() = debugException {
            driver.findElement(By.id("bets"))
                .findElements(By.tagName("li"))
                .associate {
                    val (name, bet) = it.text.split(":")
                        .apply { if (size < 2) return@associate this[0] to Bid.None }
                    if (bet == "has bet") return@associate name to Bid.IsHidden
                    name to Bid.Placed(bet.toInt())
                }
        }

    private fun <T> debugException(block: () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            printBody()
            throw e
        }
    }

    private fun <T> debugAlways(block: () -> T): T {
        printBody()
        return block()
    }

    private fun printBody() {
        println("===$playerId's view===")
        driver.findElement(By.tagName("body")).getAttribute("outerHTML").let(::println)
        println("====================")
    }
}

private fun WebElement.toCardId(): CardId = text.removeSuffix("Play")
