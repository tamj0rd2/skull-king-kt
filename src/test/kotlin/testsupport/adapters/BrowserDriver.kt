package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.Command
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ApplicationDriver
import java.time.Instant.now

private const val debug = true

class BrowserDriver(private val driver: ChromeDriver) : ApplicationDriver {
    private var playerId = PlayerId.unidentified

    init {
        withClue("the game page didn't load successfully") {
            driver.title shouldBe "Playing Skull King"
        }
    }

    override fun perform(command: Command.PlayerCommand) {
        withClue("another command is still in progress") {
            noCommandsAreInProgress shouldBe true
        }

        when (command) {
            is Command.PlayerCommand.JoinGame -> joinGame(command.actor)
            is Command.PlayerCommand.PlaceBid -> bid(command.bid.bid)
            is Command.PlayerCommand.PlayCard -> playCard(command.cardName)
        }

        waitUntil({ noCommandsAreInProgress }, "the command is still in progress")
    }

    private fun joinGame(playerId: PlayerId) = debugException {
        this.playerId = playerId
        driver.findElement(By.name("playerId")).sendKeys(playerId.playerId)
        driver.findElement(By.id("joinGame")).submit()
        waitUntil({ driver.findElement(By.tagName("h1")).text == "Game Page - $playerId" })
    }

    private fun bid(bid: Int) = debugException {
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

    private fun playCard(cardName: CardName) = debugException {
        val li = driver.findElement(By.id("hand"))
            .findElements(By.tagName("li"))
            .ifEmpty { error("$playerId has no cards") }
            .find { it.toCard().name == cardName }
            .let { it ?: error("$playerId does not have card $cardName") }

        try {
            li.findElement(By.tagName("button")).click()
        } catch (e: NoSuchElementException) {
            throw GameException.CannotPlayCard(e.message)
        }
    }

    override val winsOfTheRound: Map<PlayerId, Int>
        get() = debugException {
            driver.findElement(By.id("wins"))
                .findElements(By.tagName("li"))
                .associate {
                    val playerId = it.getAttribute("data-playerId").let(::PlayerId)
                    val wins = it.getAttribute("data-wins").toInt()
                    playerId to wins
                }
        }

    override val trickWinner: PlayerId?
        get() = debugException {
            driver.findElement(By.id("trickWinner")).getAttributeOrNull("data-playerId")?.let(::PlayerId)
        }

    override val trickNumber: Int?
        get() = debugException {
            driver.findElement(By.id("trickNumber")).getAttributeOrNull("data-trickNumber")?.toInt()
        }

    override val roundNumber: Int?
        get() = debugException {
            driver.findElement(By.id("roundNumber")).getAttributeOrNull("data-roundNumber")?.toInt()
        }

    override val playersInRoom: List<PlayerId>
        get() = debugException {
            driver.findElement(By.id("players"))
                .findElements(By.tagName("li"))
                .map { PlayerId(it.text) }
        }

    override val hand: List<CardWithPlayability>
        get() = debugException {
            driver.findElement(By.id("hand"))
                .findElements(By.tagName("li"))
                .map { it.toCardWithPlayability() }
        }

    override val trick: List<PlayedCard>
        get() = debugException {
            driver.findElement(By.id("trick"))
                .findElements(By.tagName("li"))
                .map { it.toCard().playedBy(PlayerId(it.getAttribute("player"))) }
        }

    override val gameState: GameState?
        get() = debugException {
            driver.findElementOrNull(By.id("gameState"))
                ?.getAttributeOrNull("data-state")
                ?.let(GameState::from)
        }

    override val roundPhase: RoundPhase?
        get() = debugException {
            driver.findElementOrNull(By.id("roundPhase"))
                ?.getAttributeOrNull("data-phase")
                ?.let(RoundPhase::from)
        }

    override val bids: Map<PlayerId, DisplayBid>
        get() = debugException {
            driver.findElement(By.id("bids"))
                .findElements(By.tagName("li"))
                .associate {
                    val (name, bid) = it.text.split(":")
                        .apply { if (size < 2) return@associate PlayerId(this[0]) to DisplayBid.None }
                    val playerId = PlayerId(name)
                    if (bid == "has bid") return@associate playerId to DisplayBid.Hidden
                    playerId to DisplayBid.Placed(bid.toInt())
                }
        }

    override val currentPlayer: PlayerId?
        get() = debugException {
            driver.findElement(By.id("currentPlayer")).getAttribute("data-playerId")?.let(::PlayerId)
                ?: error("no player id for the current player")
        }

    private val noCommandsAreInProgress get() = !driver.findElement(By.id("spinner")).isDisplayed

    private fun WebElement.getAttributeOrNull(attributeName: String): String? = getAttribute(attributeName)

    private fun WebDriver.findElementOrNull(by: By): WebElement? = try {
        findElement(by)
    } catch (e: NoSuchElementException) {
        null
    }

    private fun WebElement.findElementOrNull(by: By): WebElement? = try {
        findElement(by)
    } catch (e: NoSuchElementException) {
        null
    }


    private fun <T> debugException(block: () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            if (debug) printContent()
            throw e
        }
    }

    private fun <T> debugAlways(block: () -> T): T {
        printContent()
        return block()
    }

    private fun printContent() {
        val root = driver.findElementOrNull(By.id("root")) ?: driver.findElement(By.tagName("body"))
        val html = root.getAttribute("outerHTML")
        println(
            """
            |===========$playerId's view===========
            |$html
            |===========end of $playerId's view===========
        """.trimMargin()
        )
    }

    private fun WebElement.toCard() = Card.from(this.getAttribute("suit"), this.getAttribute("number")?.toInt())
    private fun WebElement.toCardWithPlayability(): CardWithPlayability {
        val isPlayable = findElementOrNull(By.tagName("button")) != null
        return CardWithPlayability(toCard(), isPlayable)
    }
}

private fun waitUntil(predicate: () -> Boolean, errorMessage: String = "predicate not met within timeout") {
    val mustEndBy = now().plusSeconds(1)
    do {
        if (predicate()) return
    } while (mustEndBy > now())
    error(errorMessage)
}
