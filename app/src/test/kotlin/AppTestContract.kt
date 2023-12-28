
import TestHelpers.playUpTo
import TestHelpers.playUpToStartOf
import TestHelpers.skipToTrickTaking
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.DisplayBid.*
import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.GameState.InProgress
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.RoundPhase.*
import com.tamj0rd2.domain.blue
import com.tamj0rd2.domain.red
import org.junit.jupiter.api.Nested
import testsupport.Actor
import testsupport.Bid
import testsupport.Bidding
import testsupport.Bids
import testsupport.Ensurer
import testsupport.HerHand
import testsupport.HisHand
import testsupport.Is
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.Play
import testsupport.Playing
import testsupport.Plays
import testsupport.Question
import testsupport.RigsTheDeck
import testsupport.SaysTheGameCanStart
import testsupport.SaysTheNextTrickCanStart
import testsupport.SaysTheRoundCanStart
import testsupport.SaysTheTrickCanStart
import testsupport.SitAtTheTable
import testsupport.SitsAtTheTable
import testsupport.SittingAtTheTable
import testsupport.TheCurrentPlayer
import testsupport.TheCurrentTrick
import testsupport.TheGameState
import testsupport.ThePlayersAtTheTable
import testsupport.TheRoundNumber
import testsupport.TheRoundPhase
import testsupport.TheTrickNumber
import testsupport.TheWinnerOfTheTrick
import testsupport.TheirFirstCard
import testsupport.TheirHand
import testsupport.TheySeeBids
import testsupport.TheySeeWinsOfTheRound
import testsupport.annotations.AutomatedGameMasterTests
import testsupport.annotations.UnhappyPathTest
import testsupport.annotations.Wip
import testsupport.are
import testsupport.both
import testsupport.each
import testsupport.ensurer
import testsupport.expectingFailure
import testsupport.isEmpty
import testsupport.playerId
import testsupport.sizeIs
import testsupport.wouldFailBecause
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration

interface AbilityFactory {
    fun participateInGames(): ParticipateInGames
    fun manageGames(): ManageGames
}

interface TestConfiguration : AbilityFactory {
    fun setup()
    fun teardown()
    val automaticGameMasterDelay: Duration
    fun automateGameMasterCommands() {
        TODO("automateGameMasterCommands not implemented")
    }
}

// things for the future:
// TODO: turn order
// TODO: scoring

abstract class AppTestContract(private val c: TestConfiguration) : Ensurer by ensurer() {
    private val freddy by lazy { Actor("Freddy First").whoCan(c.participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(c.participateInGames()) }

    @BeforeTest
    fun setup() = c.setup()

    @AfterTest
    fun teardown() = c.teardown()

    @Nested
    @Suppress("unused")
    inner class GivenThereIsAManualGameMaster {

        private val gary by lazy { Actor("Gary GameMaster").whoCan(c.manageGames()) }

        @Test
        fun `sitting at an empty table and waiting for more players to join`() {
            freddy(
                SitsAtTheTable,
                ensures {
                    that(ThePlayersAtTheTable, are(freddy))
                    that(TheGameState, Is(GameState.WaitingForMorePlayers))
                },
            )
        }

        @Wip
        @Test
        fun `waiting for sally to bid`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy and sally both ensure(TheRoundPhase, Is(Bidding))
            freddy(Bids(1))
            freddy and sally both ensure {
                that(TheRoundPhase, Is(Bidding))
                that(TheySeeBids, where(freddy.bidIsHidden(), sally.hasNotBid()))
            }
        }

        @Wip
        @Test
        fun `playing a card and waiting for the next player to do the same`() {
            freddy and sally both SitAtTheTable
            gary(RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue), SaysTheGameCanStart)
            freddy and sally both Bid(1)
            freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))

            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }

            freddy(
                ensures(HisHand, sizeIs(1)),
                Plays(11.blue),
                ensures(HisHand, isEmpty()),
            )

            freddy and sally both ensure {
                that(TheCurrentTrick, onlyContains(11.blue.playedBy(freddy)))
                that(TheRoundPhase, Is(TrickTaking))
                that(TheCurrentPlayer, Is(sally.playerId))
            }
        }

        @Wip
        @Test
        fun `winning a trick`() {
            freddy and sally both SitAtTheTable
            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(12.blue),
                SaysTheGameCanStart
            )
            freddy and sally both Bid(1)
            freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))

            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }

            freddy and sally both Play.theirFirstPlayableCard

            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickCompleted))
                that(TheWinnerOfTheTrick, Is(sally.playerId))
                that(TheySeeWinsOfTheRound, where(freddy won 0, sally won 1))
            }

            // testing for round 2
            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue, 12.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(1.blue, 2.blue),
            )
            skipToTrickTaking(theGameMaster = gary, thePlayers = listOf(freddy, sally))

            freddy and sally both ensure {
                that(TheRoundNumber, Is(2))
                that(TheRoundPhase, Is(TrickTaking))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }

            freddy and sally both Play.theirFirstPlayableCard
            gary(SaysTheNextTrickCanStart)
            freddy and sally both Play.theirFirstPlayableCard

            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickCompleted))
                that(TheySeeWinsOfTheRound, where(freddy won 2, sally won 0))
            }
        }

        @UnhappyPathTest
        fun `cannot play a card when the trick is not in progress`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)

            freddy(Bids(1))
            freddy and sally both ensure(TheRoundPhase, Is(Bidding))
            freddy and sally both Play.theirFirstPlayableCard.expectingFailure(GameErrorCode.TrickNotInProgress)

            sally(Bids(1))
            freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))
            freddy and sally both Play.theirFirstPlayableCard.expectingFailure(GameErrorCode.TrickNotInProgress)
        }

        @UnhappyPathTest
        fun `cannot play a card that would break suit rules`() {
            val thePlayers = listOf(freddy, sally)
            val theGameMaster = gary

            playUpTo(endOfRound = 1, theGameMaster = theGameMaster, thePlayers = thePlayers)

            theGameMaster(
                RigsTheDeck.SoThat(freddy).willEndUpWith(1.blue, 2.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(3.blue, 4.red),
            )
            skipToTrickTaking(theGameMaster = theGameMaster, thePlayers = thePlayers)

            freddy(Plays(1.blue))
            sally(Playing(4.red) wouldFailBecause GameErrorCode.PlayingCardWouldBreakSuitRules)

            // recovery
            sally(ensures(HerHand, sizeIs(2)))
            sally(Plays(3.blue))

            theGameMaster(SaysTheNextTrickCanStart)
            freddy(Plays(2.blue))
            sally(Plays(4.red))
        }

        @Wip
        @Test
        fun `can play special cards when you have a card for the correct suit - sanity check`() {
            val thePlayers = listOf(freddy, sally)
            val theGameMaster = gary

            playUpTo(endOfRound = 1, theGameMaster = theGameMaster, thePlayers = thePlayers)

            theGameMaster(
                RigsTheDeck.SoThat(freddy).willEndUpWith(1.blue, 2.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(3.blue, Card.SpecialCard.escape),
            )
            skipToTrickTaking(theGameMaster = theGameMaster, thePlayers = thePlayers)

            freddy(Plays(1.blue))
            sally(Plays(Card.SpecialCard.escape))
            thePlayers each ensure(TheRoundPhase, Is(TrickCompleted))
        }

        @UnhappyPathTest
        fun `cannot play a card when it is not their turn`() {
            val thirzah = Actor("Thirzah Third").whoCan(c.participateInGames())
            val thePlayers = listOf(freddy, sally, thirzah)
            playUpToStartOf(round = 2, trick = 1, theGameMaster = gary, thePlayers = thePlayers)

            thePlayers each ensure(TheCurrentPlayer, Is(freddy.playerId))
            sally(Playing.theirFirstPlayableCard wouldFailBecause GameErrorCode.NotYourTurn)

            thePlayers each ensure(TheCurrentPlayer, Is(freddy.playerId))
            freddy(Plays.theirFirstPlayableCard)

            thePlayers each ensure(TheCurrentPlayer, Is(sally.playerId))
            thirzah(Playing.theirFirstPlayableCard wouldFailBecause GameErrorCode.NotYourTurn)

            // recovery
            thePlayers each ensure(TheCurrentPlayer, Is(sally.playerId))
            sally(Plays.theirFirstPlayableCard)
            thirzah(Plays.theirFirstPlayableCard)
        }

        @UnhappyPathTest
        fun `cannot bid before the game has started`() {
            freddy and sally both SitAtTheTable
            freddy(Bidding(1) wouldFailBecause GameErrorCode.GameNotInProgress)
            freddy and sally both ensure(TheGameState, Is(GameState.WaitingToStart))
        }

        @UnhappyPathTest
        fun `cannot bid outside of the bidding phase`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy and sally both Bid(1)
            freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure(TheRoundPhase, Is(TrickTaking))
            freddy(Bidding(1) wouldFailBecause GameErrorCode.BiddingIsNotInProgress)
        }

        @UnhappyPathTest
        fun `cannot bid twice`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy(Bids(1))
            freddy(Bidding(1) wouldFailBecause GameErrorCode.AlreadyPlacedABid)
        }

        @UnhappyPathTest
        fun `cannot bid more than the current round number`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy(Bidding(2) wouldFailBecause GameErrorCode.BidLessThan0OrGreaterThanRoundNumber)
        }

        @UnhappyPathTest
        fun `a player can't join twice`() {
            freddy(SitsAtTheTable)
            val freddyOnASecondDevice = Actor(freddy.name).whoCan(c.participateInGames())
            freddyOnASecondDevice(SittingAtTheTable wouldFailBecause GameErrorCode.PlayerWithSameNameAlreadyInGame)
        }

        @Wip
        @Test
        fun `playing a game from start to finish`() {
            freddy and sally both SitAtTheTable
            freddy and sally both ensure {
                that(ThePlayersAtTheTable, are(freddy, sally))
                that(TheGameState, Is(GameState.WaitingToStart))
            }

            // round 1
            gary(SaysTheGameCanStart)
            freddy and sally both ensure {
                that(TheRoundNumber, Is(1))
                that(TheirHand, sizeIs(1))
                that(TheRoundPhase, Is(Bidding))
            }

            // round 1 bidding
            freddy and sally both Bid(1)
            freddy and sally both ensure {
                that(TheySeeBids, where(freddy bid 1, sally bid 1))
                that(TheRoundPhase, Is(BiddingCompleted))
            }

            // round 1 trick taking
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheTrickNumber, Is(1))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }

            val freddysCard = freddy.asksAbout(TheirFirstCard).also { freddy(Plays(it)) }
            val sallysCard = sally.asksAbout(TheirFirstCard).also { sally(Plays(it)) }
            freddy and sally both ensure {
                that(TheCurrentTrick, onlyContains(freddysCard.playedBy(freddy), sallysCard.playedBy(sally)))
                that(TheRoundPhase, Is(TrickCompleted))
                that(TheirHand, isEmpty())
            }

            // round 2
            gary(SaysTheRoundCanStart)
            freddy and sally both ensure {
                that(TheRoundNumber, Is(2))
                that(TheirHand, sizeIs(2))
                that(TheRoundPhase, Is(Bidding))
            }

            // round 2 bidding
            freddy and sally both Bid(2)
            freddy and sally both ensure {
                that(TheySeeBids, where(freddy bid 2, sally bid 2))
                that(TheRoundPhase, Is(BiddingCompleted))
            }

            // round 2 trick 1
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheTrickNumber, Is(1))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }
            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure {
                that(TheCurrentTrick, sizeIs(2))
                that(TheRoundPhase, Is(TrickCompleted))
                that(TheirHand, sizeIs(1))
            }

            // round 2 trick 2
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheTrickNumber, Is(2))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure {
                that(TheCurrentTrick, sizeIs(2))
                that(TheRoundPhase, Is(TrickCompleted))
                that(TheirHand, isEmpty())
            }

            // rounds 3 - 10
            (3..10).forEach { roundNumber ->
                // round X
                gary(SaysTheRoundCanStart)
                freddy and sally both ensure {
                    that(TheRoundNumber, Is(roundNumber))
                    that(TheirHand, sizeIs(roundNumber))
                }

                // round X bidding
                freddy and sally both Bid(roundNumber)
                freddy and sally both ensure {
                    that(TheySeeBids, where(freddy bid roundNumber, sally bid roundNumber))
                    that(TheRoundPhase, Is(BiddingCompleted))
                }

                // round X trick 1-X
                (1..roundNumber).forEach { trickNumber ->
                    gary(SaysTheTrickCanStart)
                    freddy and sally both ensure {
                        that(TheRoundPhase, Is(TrickTaking))
                        that(TheTrickNumber, Is(trickNumber))
                        that(TheirHand, sizeIs(roundNumber - trickNumber + 1))
                        that(TheCurrentPlayer, Is(freddy.playerId))
                    }

                    freddy and sally both Play.theirFirstPlayableCard
                    freddy and sally both ensure {
                        that(TheCurrentTrick, sizeIs(2))
                        that(TheRoundPhase, Is(TrickCompleted))
                        that(TheirHand, sizeIs(roundNumber - trickNumber))
                    }
                }
            }

            freddy and sally both ensure {
                that(TheRoundNumber, Is(10))
                that(TheTrickNumber, Is(10))
                that(TheirHand, isEmpty())
                that(TheGameState, Is(GameState.Complete))
            }
        }
    }

    @Nested
    @Suppress("unused")
    @AutomatedGameMasterTests
    @Wip
    inner class GivenAutomatedGameMasterCommandsAreEnabled {
        init {
            c.automateGameMasterCommands()
        }

        private val gmDelay = c.automaticGameMasterDelay
        private val withinDelay = gmDelay * 2

        @Test
        fun `the game automatically starts after a delay when the minimum table size is reached`() {
            freddy and sally both SitAtTheTable
            freddy and sally both ensure(ThePlayersAtTheTable, are(freddy, sally))
            freddy and sally both ensure(within = withinDelay) {
                that(TheGameState, Is(InProgress))
                that(TheRoundNumber, Is(1))
                that(TheirHand, sizeIs(1))
                that(TheRoundPhase, Is(Bidding))
            }
        }

        @Test
        fun `the auto-start of the game still allows for other people to join`() {
            val thirzah = Actor("Thirzah Third").whoCan(c.participateInGames())

            freddy and sally both SitAtTheTable
            Thread.sleep((gmDelay / 4).inWholeMilliseconds)
            thirzah(SitsAtTheTable)

            freddy and sally and thirzah each ensure(ThePlayersAtTheTable, are(freddy, sally, thirzah))
            freddy and sally and thirzah each ensure(within = withinDelay) {
                that(TheGameState, Is(InProgress))
                that(TheRoundNumber, Is(1))
                that(TheirHand, sizeIs(1))
                that(TheRoundPhase, Is(Bidding))
            }
        }

        @Test
        fun `when all players have bid, the trick automatically begins after a delay`() {
            freddy and sally both SitAtTheTable
            freddy and sally both ensure(TheRoundPhase, Is(Bidding), within = withinDelay)

            freddy and sally both Bid(1)
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheTrickNumber, Is(1))
                that(TheCurrentPlayer, Is(freddy.playerId))
            }
        }

        @Test
        fun `when all players have played their card, the next trick or round automatically begins`() {
            data class RoundState(val round: Int?, val phase: RoundPhase?, val trick: Int? = null)

            val TheRoundState = Question("about the round state") { actor ->
                RoundState(
                    round = actor.asksAbout(TheRoundNumber),
                    phase = actor.asksAbout(TheRoundPhase),
                    trick = actor.asksAbout(TheTrickNumber)
                )
            }

            freddy and sally both SitAtTheTable
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 1, phase = Bidding)))
            }

            freddy and sally both Bid(1)
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 1, phase = TrickTaking, trick = 1)))
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 2, phase = Bidding)))
            }

            freddy and sally both Bid(1)
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 2, phase = TrickTaking, trick = 1)))
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 2, phase = TrickTaking, trick = 2)))
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(within = withinDelay) {
                that(TheRoundState, Is(RoundState(round = 3, phase = Bidding)))
            }
        }
    }
}

private infix fun Actor.won(count: Int) = Pair(this, count)

internal object TestHelpers: Ensurer by ensurer() {
    fun playUpToStartOf(round: Int, trick: Int, theGameMaster: Actor, thePlayers: List<Actor>) {
        require(round != 1) { "playing to round 1 not implemented" }
        require(trick == 1) { "trick greater than 1 not implemented" }

        playUpTo(round - 1, theGameMaster, thePlayers)
        theGameMaster(SaysTheRoundCanStart)
        thePlayers each ensure {
            that(TheRoundNumber, Is(round))
            that(TheRoundPhase, Is(Bidding))
        }

        thePlayers each Bid(1)
        playUpToAndIncluding(trick - 1, theGameMaster, thePlayers)
        theGameMaster(SaysTheNextTrickCanStart)

        thePlayers each ensure {
            that(TheRoundNumber, Is(round))
            that(TheRoundPhase, Is(TrickTaking))
            that(TheTrickNumber, Is(trick))
        }
    }

    fun skipToTrickTaking(theGameMaster: Actor, thePlayers: List<Actor>) {
        val gameState = thePlayers.first().asksAbout(TheGameState)
        require(gameState == InProgress) { "cannot skip to trick taking when the game is $gameState" }

        when (thePlayers.first().asksAbout(TheRoundPhase)) {
            Bidding -> {
                thePlayers each Bid(1)
                theGameMaster(SaysTheTrickCanStart)
            }

            BiddingCompleted -> theGameMaster(SaysTheTrickCanStart)
            TrickTaking -> error("already in the trick taking phase")
            TrickCompleted -> {
                val trickNumber = thePlayers.first().asksAbout(TheTrickNumber)
                val roundNumber = thePlayers.first().asksAbout(TheRoundNumber)
                if (trickNumber == roundNumber) theGameMaster(SaysTheRoundCanStart)
                thePlayers each ensure(TheRoundPhase, Is(Bidding))
                thePlayers each Bid(1)
                theGameMaster(SaysTheTrickCanStart)
            }

            null -> error("the game probably hasn't started yet")
        }

        thePlayers each ensure {
            that(TheRoundPhase, Is(TrickTaking))
            that(TheTrickNumber, Is(1))
        }
    }

    fun playUpTo(endOfRound: Int, theGameMaster: Actor, thePlayers: List<Actor>) {
        thePlayers each SitAtTheTable
        theGameMaster(SaysTheGameCanStart)

        (1..endOfRound).forEach { roundNumber ->
            if (roundNumber != 1) {
                theGameMaster(SaysTheRoundCanStart)
            }

            thePlayers each ensure {
                that(TheRoundNumber, Is(roundNumber))
                that(TheRoundPhase, Is(Bidding))
            }
            thePlayers each Bid(1)
            playUpToAndIncluding(trick = roundNumber, theGameMaster = theGameMaster, thePlayers = thePlayers)
        }
    }

    fun playUpToAndIncluding(trick: Int, theGameMaster: Actor, thePlayers: List<Actor>) {
        require(
            thePlayers.first().asksAbout(TheRoundPhase) == BiddingCompleted
        ) { "must start from bidding completion" }
        if (trick < 1) return

        (1..trick).forEach { trickNumber ->
            theGameMaster(SaysTheTrickCanStart)
            thePlayers each ensure {
                that(TheRoundPhase, Is(TrickTaking))
                that(TheTrickNumber, Is(trickNumber))
            }

            thePlayers each Play.theirFirstPlayableCard
            thePlayers each ensure {
                that(TheRoundPhase, Is(TrickCompleted))
            }
        }
    }
}

internal fun Card.playedBy(actor: Actor): PlayedCard = this.playedBy(actor.playerId)

internal infix fun Actor.and(other: Actor) = this to other
internal infix fun Pair<Actor, Actor>.and(other: Actor) = listOf(first, second, other)

infix fun Actor.bid(bid: Int): Pair<Actor, DisplayBid> = Pair(this, Placed(com.tamj0rd2.domain.Bid(bid)))
fun Actor.bidIsHidden(): Pair<Actor, DisplayBid> = Pair(this, Hidden)
fun Actor.hasNotBid(): Pair<Actor, DisplayBid> = Pair(this, None)
