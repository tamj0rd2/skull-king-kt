package testsupport.adapters

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.Trick
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ApplicationDriver

private const val debug = false
class WebDriver(private val driver: ChromeDriver) : ApplicationDriver {
    private lateinit var playerId: String

    override fun joinGame(playerId: PlayerId) = debugException {
        driver.findElement(By.name("playerId")).sendKeys(playerId)
        this.playerId = playerId

        driver.findElement(By.id("joinGame")).submit().apply {
            val errorElements = driver.findElements(By.id("errorMessage"))
            if (errorElements.isNotEmpty()) {
                when (val errorMessage = errorElements.single().text) {
                    // TODO: gross... just have a data attribute with the code on the page, rather than this.
                    GameException.PlayerWithSameNameAlreadyJoined::class.simpleName!! -> throw GameException.PlayerWithSameNameAlreadyJoined(
                        playerId
                    )

                    else -> error("unknown error message: $errorMessage")
                }
            }
        }
    }

    override fun bid(bid: Int) = debugException {
        try {
            driver.findElement(By.name("bid")).sendKeys(bid.toString())
            driver.findElement(By.id("placeBid")).let {
                if (!it.isEnabled) throw GameException.CannotBid("bid button is disabled")
                it.click()
            }
        } catch (e: NoSuchElementException) {
            throw GameException.CannotBid(e.message)
        }
        // TODO: is there a nicer way to deal with this?
        // give some time for the server to respond if necessary
        Thread.sleep(10)
    }

    override fun playCard(card: Card) = debugException {
        driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .ifEmpty { error("$playerId has no cards") }
            .find { it.toCard().name == card.name }
            .let { it ?: error("$playerId does not have card $card") }
            .findElement(By.tagName("button"))
            .click()
    }

    override val trickNumber: Int get() = debugException {
        driver.findElement(By.id("trickNumber")).getAttribute("data-trickNumber").toInt()
    }

    override val roundNumber: Int get() = debugException {
        driver.findElement(By.id("roundNumber")).getAttribute("data-roundNumber").toInt()
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
                .map { it.toCard() }
        }

    override val trick: Trick
        get() = debugException {
            driver.findElement(By.id("trick"))
                .findElements(By.tagName("li"))
                .map { it.toCard().playedBy(it.getAttribute("player")) }
        }

    override val gameState: GameState
        get() = debugException {
            // TODO: this needs updating...
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
            val rawPhase = driver.findElement(By.id("gamePhase")).getAttribute("data-phase")
            RoundPhase.from(rawPhase)
        }

    override val bids: Map<PlayerId, Bid>
        get() = debugException {
            driver.findElement(By.id("bids"))
                .findElements(By.tagName("li"))
                .associate {
                    val (name, bid) = it.text.split(":")
                        .apply { if (size < 2) return@associate this[0] to Bid.None }
                    if (bid == "has bid") return@associate name to Bid.IsHidden
                    name to Bid.Placed(bid.toInt())
                }
        }

    private fun <T> debugException(block: () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            if (debug) printBody()
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

    private fun WebElement.toCard() = Card.from(this.getAttribute("suit"), this.getAttribute("number")?.toInt())
}
