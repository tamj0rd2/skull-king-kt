package testsupport.adapters

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.tamj0rd2.domain.*
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testsupport.ApplicationDriver
import testsupport.keepTrying
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val debug = true

class BrowserDriver(private val driver: ChromeDriver) : ApplicationDriver {
    private var playerId = PlayerId.unidentified

    init {
        expectThat(driver.title).isEqualTo("Playing Skull King")
    }

    override val state: PlayerState
        get() =
            PlayerState(
                playerId = playerId,
                winsOfTheRound = winsOfTheRound,
                trickWinner = trickWinner,
                currentPlayer = currentPlayer,
                trickNumber = trickNumber,
                roundNumber = roundNumber,
                trick = trick?.let { Trick(playersInRoom.size, it) },
                roundPhase = roundPhase,
                gamePhase = gamePhase,
                playersInRoom = playersInRoom,
                hand = hand,
                bids = bids,
                // TODO: what should I do here? I suppose the answer is - represent it on the frontend
                turnOrder = emptyList(),
            )

    override fun perform(command: PlayerCommand): Result<Unit, CommandError> {
        expectThat(noCommandsAreInProgress).isEqualTo(true)

        when (command) {
            is PlayerCommand.JoinGame -> joinGame(command.actor)
            is PlayerCommand.PlaceBid -> bid(command.bid.value)
            is PlayerCommand.PlayCard -> playCard(command.cardName)
        }

        expectThat(noCommandsAreInProgress).isEqualTo(true)
        return Ok(Unit)
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

            val title = driver.findElement(By.tagName("h1")).text
            expectThat(title).isEqualTo("Game Page - $playerId")
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
                gamePhase == GamePhase.WaitingToStart -> GameErrorCode.GameNotInProgress
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

    private val winsOfTheRound: Map<PlayerId, Int>
        get() = debugException {
            (driver.findElementOrNull(By.id("wins")) ?: return@debugException emptyMap())
                .findElements(By.tagName("li"))
                .associate {
                    val playerId = it.getAttribute("data-playerId").let(::PlayerId)
                    val wins = it.getAttribute("data-wins").toInt()
                    playerId to wins
                }
        }

    private val trickWinner: PlayerId?
        get() = debugException {
            driver.findElementOrNull(By.id("trickWinner"))?.getAttributeOrNull("data-playerId")?.let(::PlayerId)
        }

    private val trickNumber: TrickNumber
        get() = debugException {
            driver.findElementOrNull(By.id("trickNumber"))
                ?.getAttributeOrNull("data-trickNumber")
                ?.let(TrickNumber::parse)
                ?: TrickNumber.None
        }

    private val roundNumber: RoundNumber
        get() = debugException {
            driver.findElementOrNull(By.id("roundNumber"))
                ?.getAttributeOrNull("data-roundNumber")
                ?.let(RoundNumber::parse)
                ?: RoundNumber.None
        }

    private val playersInRoom: List<PlayerId>
        get() = debugException {
            (driver.findElementOrNull(By.id("players")) ?: return@debugException emptyList())
                .findElements(By.tagName("li"))
                .mapNotNull { PlayerId(it.text) }
        }

    private val hand: List<CardWithPlayability>
        get() = debugException {
            (driver.findElementOrNull(By.id("hand")) ?: return@debugException emptyList())
                .findElements(By.tagName("li"))
                .map { it.toCardWithPlayability() }
        }

    private val trick: List<PlayedCard>?
        get() = debugException {
            driver.findElementOrNull(By.id("trick"))
                ?.findElements(By.tagName("li"))
                ?.map { it.toCard().playedBy(PlayerId(it.getAttribute("player"))) }
        }

    private val gamePhase: GamePhase?
        get() = debugException {
            driver.findElementOrNull(By.id("gamePhase"))?.text?.let(GamePhase::from)
        }

    private val roundPhase: RoundPhase?
        get() = debugException {
            driver.findElementOrNull(By.id("roundPhase"))?.text?.let(RoundPhase::from)
        }

    private val bids: Map<PlayerId, DisplayBid>
        get() = debugException {
            (driver.findElementOrNull(By.id("bids")) ?: return@debugException emptyMap())
                .findElements(By.tagName("li"))
                .associate {
                    val playerId = it.getAttributeOrNull("data-playerId")?.let(::PlayerId) ?: error("no playerId")
                    val bid = it.getAttributeOrNull("data-bid")?.let(DisplayBid::parse) ?: error("no bid")
                    playerId to bid
                }
        }

    private val currentPlayer: PlayerId?
        get() = debugException {
            driver.findElementAttributeOrNull(By.id("currentPlayer"), "data-playerId")?.let(::PlayerId)
        }

    private fun WebDriver.findElementAttributeOrNull(by: By, attributeName: String): String? =
        findElementOrNull(by)?.getAttributeOrNull(attributeName)

    private val noCommandsAreInProgress
        get() = driver.findElementOrNull(By.id("spinner"))?.let { !it.isDisplayed } ?: true

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


private fun <T> eventually(fn: () -> T) = keepTrying(2.seconds, 100.milliseconds, fn)