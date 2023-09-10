package testsupport.adapters

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayedCard
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
    }

    override fun playCard(cardId: CardId) = debugException {
        driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .find { it.toCardId() == cardId }
            .let { it ?: error("$playerId does not have card $cardId") }
            .findElement(By.tagName("button"))
            .click()
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
}

private fun WebElement.toCardId(): CardId = text.removeSuffix("Play")
