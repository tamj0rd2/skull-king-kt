
import TestHelpers.playUpTo
import TestHelpers.playUpToStartOf
import TestHelpers.skipToTrickTaking
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.DisplayBid.*
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState.*
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.RoundPhase.*
import com.tamj0rd2.domain.blue
import com.tamj0rd2.domain.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import testsupport.Activity
import testsupport.Actor
import testsupport.Bid
import testsupport.Bids
import testsupport.EnsureActivity
import testsupport.HerFirstCard
import testsupport.HerHand
import testsupport.HisFirstCard
import testsupport.HisHand
import testsupport.Is
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.Play
import testsupport.Plays
import testsupport.RigsTheDeck
import testsupport.SaysTheGameCanStart
import testsupport.SaysTheNextTrickCanStart
import testsupport.SaysTheRoundCanStart
import testsupport.SaysTheTrickCanStart
import testsupport.SitAtTheTable
import testsupport.SitsAtTheTable
import testsupport.TheCurrentPlayer
import testsupport.TheCurrentTrick
import testsupport.TheGameState
import testsupport.ThePlayersAtTheTable
import testsupport.TheRoundNumber
import testsupport.TheRoundPhase
import testsupport.TheTrickNumber
import testsupport.TheWinnerOfTheTrick
import testsupport.TheirHand
import testsupport.TheySeeBids
import testsupport.TheySeeWinsOfTheRound
import testsupport.ensure
import testsupport.ensures
import testsupport.expectingFailure
import testsupport.isEmpty
import testsupport.playerId
import testsupport.sizeIs
import testsupport.waitUntil
import java.lang.management.ManagementFactory
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
}

// things for the future:
// TODO: turn order
// TODO: scoring

sealed class AppTestContract(protected val c: TestConfiguration) {
    protected val freddy by lazy { Actor("Freddy First").whoCan(c.participateInGames()) }
    protected val sally by lazy { Actor("Sally Second").whoCan(c.participateInGames()) }
    protected val gary by lazy { Actor("Gary GameMaster").whoCan(c.manageGames()) }

    @BeforeTest fun setup() = c.setup()

    @AfterTest fun teardown() = c.teardown()

    @Test
    fun `sitting at an empty table and waiting for more players to join`() {
        freddy(
            SitsAtTheTable,
            ensures {
                that(ThePlayersAtTheTable, are(freddy))
                that(TheGameState, Is(WaitingForMorePlayers))
            },
        )
    }

    @Test
    fun `waiting for sally to bid`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy(Bids(1))
        freddy and sally both ensure {
            that(TheRoundPhase, Is(Bidding))
            that(TheySeeBids, where(freddy.bidIsHidden(), sally.hasNotBid()))
        }
    }

    @Test
    fun `playing a card and waiting for the next player to do the same`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue),
            RigsTheDeck.SoThat(sally).willEndUpWith(12.blue),
            SaysTheGameCanStart
        )

        freddy and sally both waitUntil(TheRoundPhase, Is(Bidding), within = Duration.ZERO)
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

    @Test
    fun `winning a trick`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue),
            RigsTheDeck.SoThat(sally).willEndUpWith(12.blue),
            SaysTheGameCanStart
        )
        freddy and sally both waitUntil(TheRoundPhase, Is(Bidding), within = Duration.ZERO)

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

    @Test
    fun `cannot play a card before the trick begins`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)

        freddy(Bids(1))
        freddy and sally both ensure(TheRoundPhase, Is(Bidding))
        freddy and sally both Play.theirFirstPlayableCard.expectingFailure<GameException.CannotPlayCard>()

        sally(Bids(1))
        freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))
        freddy and sally both Play.theirFirstPlayableCard.expectingFailure<GameException.CannotPlayCard>()
    }

    @Test
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
        sally.attemptsTo(Play(4.red).expectingFailure<GameException.CannotPlayCard>())

        // recovery
        sally(ensures(HerHand, sizeIs(2)))
        sally(Plays(3.blue))

        theGameMaster(SaysTheNextTrickCanStart)
        freddy(Plays(2.blue))
        sally(Plays(4.red))
    }

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

    @Test
    fun `cannot play a card when it is not their turn`() {
        val thirzah = Actor("Thirzah Third").whoCan(c.participateInGames())
        val thePlayers = listOf(freddy, sally, thirzah)
        playUpToStartOf(round = 2, trick = 1, theGameMaster = gary, thePlayers = thePlayers)

        thePlayers each ensure(TheCurrentPlayer, Is(freddy.playerId))
        sally.attemptsTo(Play.theirFirstPlayableCard.expectingFailure<GameException.CannotPlayCard>())

        freddy and sally both Play.theirFirstPlayableCard

        thePlayers each ensure(TheCurrentPlayer, Is(thirzah.playerId))
        sally.attemptsTo(Play.theirFirstPlayableCard.expectingFailure<GameException.CannotPlayCard>())

        // recovery
        thirzah(Plays.theirFirstPlayableCard)
    }

    @Test
    fun `cannot bid before the game has started`() {
        freddy and sally both SitAtTheTable
        freddy.attemptsTo(Bid(1).expectingFailure<GameException.CannotBid>())
        freddy and sally both ensure(TheGameState, Is(WaitingToStart))
    }

    @Test
    fun `cannot bid while tricks are taking place`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy and sally both Bid(1)
        freddy and sally both ensure(TheRoundPhase, Is(BiddingCompleted))
        gary(SaysTheTrickCanStart)
        freddy and sally both ensure(TheRoundPhase, Is(TrickTaking))
        freddy.attemptsTo(Bid(1).expectingFailure<GameException.CannotBid>())
    }

    @Test
    fun `cannot bid twice`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy(Bids(1))
        freddy.attemptsTo(Bid(1).expectingFailure<GameException.CannotBid>())
    }

    @Test
    fun `cannot bid more than the current round number`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy.attemptsTo(Bid(2).expectingFailure<GameException.CannotBid>())
    }

    @Test
    fun `a player can't join twice`() {
        freddy(SitsAtTheTable)
        val freddyOnASecondDevice = Actor(freddy.name).whoCan(c.participateInGames())
        freddyOnASecondDevice.attemptsTo(SitAtTheTable.expectingFailure<GameException.PlayerWithSameNameAlreadyJoined>())
    }

    @RepeatedTest(30)
    fun `joining a game with both players`() {
        freddy(
            SitsAtTheTable,
            ensures(within = Duration.ZERO) {
                that(ThePlayersAtTheTable, are(freddy))
                that(TheGameState, Is(WaitingForMorePlayers))
            },
        )

        // TODO: there's occasionally a race condition here...
        sally(SitsAtTheTable)
        freddy and sally both ensure(within = Duration.ZERO) {
            that(ThePlayersAtTheTable, are(freddy, sally))
            that(TheGameState, Is(WaitingToStart))
        }
    }

    @Test
    fun `playing a game from start to finish`() {
        freddy and sally both SitAtTheTable
        freddy and sally both ensure {
            that(ThePlayersAtTheTable, are(freddy, sally))
            that(TheGameState, Is(WaitingToStart))
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

        val freddysFirstCard = freddy.asksAbout(HisFirstCard)
        val sallysFirstCard = sally.asksAbout(HerFirstCard)
        freddy and sally both Play.theirFirstPlayableCard
        freddy and sally both ensure {
            that(TheCurrentTrick, onlyContains(freddysFirstCard.playedBy(freddy), sallysFirstCard.playedBy(sally)))
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
            that(TheGameState, Is(Complete))
        }
    }
}

private infix fun Actor.won(count: Int) = Pair(this, count)

internal object TestHelpers {
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
        thePlayers each ensure(TheGameState, Is(InProgress))

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

        (1..endOfRound).forEach {roundNumber ->
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
        thePlayers each ensure(TheRoundPhase, Is(BiddingCompleted))
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

internal infix fun Pair<Actor, Actor>.both(activity: Activity) {
    listOf(first, second).each(activity)
}

val isInDebugMode = ManagementFactory.getRuntimeMXBean().inputArguments.none { it.contains("jdwp") }
internal infix fun List<Actor>.each(activity: Activity) {
    if (activity is EnsureActivity && !isInDebugMode) {
        parallelMap { actor -> actor(activity) }
            .firstOrNull { it.isFailure }
            ?.onFailure { throw Error("activity '$activity' failed", it) }

        return
    }

    forEach { actor -> actor(activity) }
}

internal infix fun Actor.and(other: Actor) = this to other
internal infix fun Pair<Actor, Actor>.and(other: Actor) = listOf(first, second, other)

// NOTE: if the compiler is randomly failing here after refactors/renaming, just run a gradle clean and it fixes it
// TODO: delete this
//fun <T> where(vararg data: Pair<Actor, T>): Matcher<Map<PlayerId, T>> =
//    equalTo(data.associate { it.first.playerId to it.second })

infix fun Actor.bid(bid: Int): Pair<Actor, DisplayBid> = Pair(this, Placed(bid))
fun Actor.bidIsHidden(): Pair<Actor, DisplayBid> = Pair(this, Hidden)
fun Actor.hasNotBid(): Pair<Actor, DisplayBid> = Pair(this, None)

private fun <A, B>List<A>.parallelMap(f: suspend (A) -> B): List<Result<B>> = runBlocking {
    map { async(Dispatchers.Default) {
        runCatching { f(it) }
    } }.map { it.await() }
}
