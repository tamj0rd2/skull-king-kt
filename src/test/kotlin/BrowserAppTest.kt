
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.Server
import org.eclipse.jetty.client.HttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import testsupport.Actor
import testsupport.Bid
import testsupport.Ensurer
import testsupport.Is
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.Play
import testsupport.Question
import testsupport.SitAtTheTable
import testsupport.SitsAtTheTable
import testsupport.SkipWip
import testsupport.TheCurrentPlayer
import testsupport.TheGameState
import testsupport.ThePlayersAtTheTable
import testsupport.TheRoundNumber
import testsupport.TheRoundPhase
import testsupport.TheTrickNumber
import testsupport.TheirHand
import testsupport.adapters.BrowserDriver
import testsupport.adapters.HTTPDriver
import testsupport.both
import testsupport.each
import testsupport.ensurer
import testsupport.logger
import testsupport.playerId
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private object Config {
    // this needs to manually be kept in line with the version in gradle.build.kts
    const val CHROME_VERSION = 114
    const val HEADLESS = true

    val loggingPreferences = LoggingPreferences().apply {
        enable(LogType.PERFORMANCE, java.util.logging.Level.ALL)
        enable(LogType.BROWSER, java.util.logging.Level.ALL)
        enable(LogType.CLIENT, java.util.logging.Level.ALL)
    }

    val chromeDriverBinary = "${System.getProperty("user.dir")}/.chromedriver/chromedriver-$CHROME_VERSION"
    val chromeOptions: ChromeOptions = ChromeOptions()
        .setBinary("${System.getProperty("user.dir")}/.chrome/chrome-$CHROME_VERSION.app/Contents/MacOS/Google Chrome for Testing")
        .apply {
            if (HEADLESS) addArguments("--headless")
            setCapability("goog:loggingPrefs", loggingPreferences)
            setCapability("goog:chromeOptions", mapOf("perfLoggingPrefs" to loggingPreferences))
        }
}

private class BrowserAppTestConfiguration(automaticGameMasterCommandDelay: Duration?) : TestConfiguration {
    override val assertionTimeout get() = 1.seconds

    private val port = ServerSocket(0).run {
        close()
        localPort
    }
    private val server = Server.make(
        port,
        hotReload = false,
        automateGameMasterCommands = automaticGameMasterCommandDelay != null,
        automaticGameMasterDelayOverride = automaticGameMasterCommandDelay,
    )
    private val baseUrl = "http://localhost:$port"
    private val httpClient = HttpClient()
    private val chromeDrivers = mutableListOf<ChromeDriver>()

    init {
        System.setProperty("webdriver.chrome.driver", Config.chromeDriverBinary)
    }

    override fun setup() {
        server.start()
        httpClient.start()
    }

    override fun teardown() {
        if (!Config.HEADLESS) Thread.sleep(5.seconds.inWholeMilliseconds)
        chromeDrivers.forEach {
            it.quit()
        }
        httpClient.stop()
        server.stop()
    }

    override fun participateInGames(): ParticipateInGames {
        val chromeDriver = ChromeDriver(Config.chromeOptions).apply {
            devTools.createSession()
            devTools.domains.events().addJavascriptExceptionListener { j: JavascriptException ->
                logger.error("JS error", j)
            }
            devTools.domains.events().addConsoleListener { println(it.messages.joinToString(" ")) }

            this.navigate().to(baseUrl)
            chromeDrivers += this
        }
        return ParticipateInGames(BrowserDriver(chromeDriver))
    }

    override fun manageGames(): ManageGames = ManageGames(HTTPDriver(baseUrl, httpClient))
}

@SkipWip
@Execution(ExecutionMode.SAME_THREAD)
class BrowserAppTest : AppTestContract(BrowserAppTestConfiguration(automaticGameMasterCommandDelay = null))

@Ignore
@Execution(ExecutionMode.CONCURRENT)
class BrowserAppTestWithAutomatedGameMasterCommands : Ensurer by ensurer(1.seconds) {
    private val gmDelay = 1.seconds
    private val expectedDelay = gmDelay * 2
    private val c = BrowserAppTestConfiguration(automaticGameMasterCommandDelay = gmDelay)

    private val freddy by lazy { Actor("Freddy First").whoCan(c.participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(c.participateInGames()) }

    @BeforeTest fun setup() = c.setup()

    @AfterTest fun teardown() = c.teardown()

    @Test
    fun `the game automatically starts after a delay when the minimum table size is reached`() {
        freddy and sally both SitAtTheTable
        freddy and sally both ensure(within = expectedDelay) {
            that(ThePlayersAtTheTable, are(freddy, sally))
            that(TheGameState, Is(GameState.InProgress))
            that(TheRoundNumber, Is(1))
            that(TheirHand, sizeIs(1))
            that(TheRoundPhase, Is(RoundPhase.Bidding))
        }
    }

    @Test
    fun `the auto-start of the game still allows for other people to join`() {
        val thirzah = Actor("Thirzah Third").whoCan(c.participateInGames())

        freddy and sally both SitAtTheTable
        Thread.sleep((gmDelay / 4).inWholeMilliseconds)
        thirzah(SitsAtTheTable)

        freddy and sally and thirzah each ensure(within = expectedDelay) {
            that(ThePlayersAtTheTable, are(freddy, sally, thirzah))
            that(TheGameState, Is(GameState.InProgress))
            that(TheRoundNumber, Is(1))
            that(TheirHand, sizeIs(1))
            that(TheRoundPhase, Is(RoundPhase.Bidding))
        }
    }

    @Test
    fun `when all players have bid, the trick automatically begins after a delay`() {
        freddy and sally both SitAtTheTable
        freddy and sally both ensure(TheRoundPhase, Is(RoundPhase.Bidding), within = expectedDelay)

        freddy and sally both Bid(1)
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundPhase, Is(RoundPhase.TrickTaking))
            that(TheTrickNumber, Is(1))
            that(TheCurrentPlayer, Is(freddy.playerId))
        }
    }

    @Test
    fun `when all players have played their card, the next trick or round automatically begins`() {
        data class RoundState(val round: Int?, val phase: RoundPhase?, val trick: Int? = null)
        val TheRoundState = Question("about the round state") {actor ->
            RoundState(
                round = actor.asksAbout(TheRoundNumber),
                phase = actor.asksAbout(TheRoundPhase),
                trick = actor.asksAbout(TheTrickNumber)
            )
        }

        freddy and sally both SitAtTheTable
        freddy and sally both ensure(within=expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 1, phase = RoundPhase.Bidding)))
        }

        freddy and sally both Bid(1)
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 1, phase = RoundPhase.TrickTaking, trick = 1)))
        }

        freddy and sally both Play.theirFirstPlayableCard
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 2, phase = RoundPhase.Bidding)))
        }

        freddy and sally both Bid(1)
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 2, phase = RoundPhase.TrickTaking, trick = 1)))
        }

        freddy and sally both Play.theirFirstPlayableCard
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 2, phase = RoundPhase.TrickTaking, trick = 2)))
        }

        freddy and sally both Play.theirFirstPlayableCard
        freddy and sally both ensure(within = expectedDelay) {
            that(TheRoundState, Is(RoundState(round = 3, phase = RoundPhase.Bidding)))
        }
    }
}
