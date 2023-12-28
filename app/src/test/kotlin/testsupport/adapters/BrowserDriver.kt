package testsupport.adapters

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import testsupport.ApplicationDriver
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val debug = true

class BrowserDriver(private val driver: ChromeDriver) : ApplicationDriver {
    private var playerId = PlayerId.unidentified

    init {
        withClue("the game page didn't load successfully") {
            driver.title shouldBe "Playing Skull King"
        }
    }

    override fun perform(command: PlayerCommand) {
        withClue("another command is still in progress") {
            noCommandsAreInProgress shouldBe true
        }

        when (command) {
            is PlayerCommand.JoinGame -> joinGame(command.actor)
            is PlayerCommand.PlaceBid -> bid(command.bid.value)
            is PlayerCommand.PlayCard -> playCard(command.cardName)
        }

        eventually {
            withClue("another command is still in progress") {
                noCommandsAreInProgress shouldBe true
            }
        }
    }

    private fun joinGame(playerId: PlayerId) = debugException {
        this.playerId = playerId
        driver.findElement(By.name("playerId")).sendKeys(playerId.value)
        driver.findElement(By.id("joinGame")).submit()

        eventually {
            driver.findElementOrNull(By.id("errorMessage"))?.let { el ->
                if (!el.isDisplayed) return@let

                el.getAttributeOrNull("errorCode")
                    ?.let { GameErrorCode.fromString(it).throwException() }
                    ?: error("failed to join the game due to unknown reasons")
            }

            driver.findElement(By.tagName("h1")).text shouldBe "Game Page - $playerId"
        }
    }

    private fun bid(bid: Int) = debugException {
        try {
            driver.findElement(By.name("bid")).sendKeys(bid.toString())
            driver.findElement(By.id("placeBid")).let {
                if (it.isEnabled) {
                    it.submit()
                    return@let
                }

                driver.findElementOrNull(By.id("biddingError"))
                    ?.getAttributeOrNull("errorCode")
                    ?.let { GameErrorCode.fromString(it).throwException() }
                    ?: error("bid button is disabled for unknown reasons")
            }
        } catch (e: NoSuchElementException) {
            when {
                gameState == GameState.WaitingToStart -> GameErrorCode.GameNotInProgress
                roundPhase != RoundPhase.Bidding -> GameErrorCode.BiddingIsNotInProgress
                bids[playerId] is DisplayBid.Hidden || bids[playerId] is DisplayBid.Placed -> GameErrorCode.AlreadyPlacedABid
                else -> error("cannot bid - ${e.rawMessage}")
            }.throwException()
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
            when {
                // TODO: probably not quite right...
                !hand.first { it.card.name == cardName }.isPlayable -> GameErrorCode.PlayingCardWouldBreakSuitRules
                else -> error("no play card button - ${e.rawMessage}")
            }.throwException()
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

    override val trickNumber: TrickNumber
        get() = debugException {
            driver.findElement(By.id("trickNumber"))
                .getAttributeOrNull("data-trickNumber")
                ?.let(TrickNumber::parse)
                ?: TrickNumber.None
        }

    override val roundNumber: RoundNumber
        get() = debugException {
            driver.findElement(By.id("roundNumber"))
                .getAttributeOrNull("data-roundNumber")
                ?.let(RoundNumber::parse)
                ?: RoundNumber.None
        }

    override val playersInRoom: List<PlayerId>
        get() = debugException {
            driver.findElement(By.id("players"))
                .findElements(By.tagName("li"))
                .mapNotNull { PlayerId(it.text) }
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
                    println("bid li: ${it.getAttribute("outerHTML")}")
                    val (name, bid) = it.text.split(":")
                        .apply { if (size < 2) return@associate PlayerId(this[0]) to DisplayBid.None }
                    val playerId = PlayerId(name)
                    if (bid == "has bid") return@associate playerId to DisplayBid.Hidden
                    playerId to DisplayBid.Placed(Bid.parse(bid))
                }
        }

    override val currentPlayer: PlayerId?
        get() = debugException {
            driver.findElementAttributeOrNull(By.id("currentPlayer"), "data-playerId")?.let(::PlayerId)
        }

    private fun WebDriver.findElementAttributeOrNull(by: By, attributeName: String): String? =
        findElementOrNull(by)?.getAttributeOrNull(attributeName)

    private val noCommandsAreInProgress get() = driver.findElementOrNull(By.id("spinner"))?.let { !it.isDisplayed } ?: true

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
        } catch (e: Throwable) {
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
        val html = root.getAttribute("innerHTML")
        println(
            """
            |===========$playerId's view===========
            |${Jsoup.parse(html).html()}
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


private fun <T> eventually(fn: () -> T) = runBlocking {
    eventually(eventuallyConfig, fn)
}

private val eventuallyConfig = eventuallyConfig {
    duration = 2.seconds
    initialDelay = 200.milliseconds
}
