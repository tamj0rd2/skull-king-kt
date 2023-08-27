import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo as Is
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GamePhase.*
import com.tamj0rd2.domain.GameState.*
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import testsupport.Activity
import testsupport.Actor
import testsupport.Bid
//import testsupport.Bid
import testsupport.Bids
import testsupport.Ensure
import testsupport.Ensures
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.PlaysCard
import testsupport.ThePlayersAtTheTable
import testsupport.ThePlayersWhoHaveBid
import testsupport.RigsTheDeck
import testsupport.TheCurrentTrick
import testsupport.TheGamePhase
import testsupport.TheGameState
import testsupport.TheirHand
import testsupport.TheySeeBids
import testsupport.SitAtTheTable
import testsupport.SitsAtTheTable
import testsupport.StartsTheGame
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

interface AbilityFactory {
    fun participateInGames(): ParticipateInGames
    fun manageGames(): ManageGames
}

interface TestConfiguration : AbilityFactory {
    fun setup()
    fun teardown()
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
sealed class AppTestContract(private val d: TestConfiguration) {
    @BeforeTest
    fun setup() = d.setup()

    @AfterTest
    fun teardown() = d.teardown()

    private val freddy = Actor("Freddy First").whoCan(d.participateInGames())
    private val sally = Actor("Sally Second").whoCan(d.participateInGames())
    private val gary = Actor("Gary GameMaster").whoCan(d.manageGames())

    @Test
    @Order(1)
    fun `sitting at an empty table`() {
        freddy(
            SitsAtTheTable,
            Ensures {
                that(ThePlayersAtTheTable, areOnly(freddy))
                that(TheGameState, Is(WaitingForMorePlayers))
            },
        )
    }

    @Test
    @Order(2)
    fun `once enough people are at the table`() {
        freddy(SitsAtTheTable)
        sally(SitsAtTheTable)
        freddy and sally both Ensure {
            that(ThePlayersAtTheTable, areOnly(freddy, sally))
            that(TheGameState, Is(WaitingToStart))
        }
    }

    @Test
    @Order(3)
    fun `staring the game`() {
        freddy and sally both SitAtTheTable
        gary(StartsTheGame)
        freddy and sally both Ensure {
            that(TheGameState, Is(InProgress))
            that(TheGamePhase, Is(Bidding))
            that(TheirHand, sizeIs(1))
        }
    }

    @Test
    @Order(4)
    fun `when everyone has bid`() {
        freddy and sally both SitAtTheTable
        gary(StartsTheGame)
        freddy(Bids(1))
        sally(Bids(2))
        freddy and sally both Ensure {
            // TODO: If I had a bids structure, I likely wouldn't need extensions to do this
            that(TheySeeBids, where(freddy bid Bid.Placed(1), sally bid Bid.Placed(2)))
            that(TheGamePhase, Is(TrickTaking))
        }
    }

    @Test
    @Order(5)
    fun `when someone hasn't bid`() {
        freddy and sally both SitAtTheTable
        gary(StartsTheGame)
        freddy(Bids(1))
        freddy and sally both Ensure {
            // TODO: maybe this can just be part of TheySeeBids? If someone hasn't bid, their bid can be null
            that(ThePlayersWhoHaveBid, areOnly(freddy.name))
            that(TheGamePhase, Is(Bidding))
            that(TheySeeBids, where(freddy bid Bid.None, sally bid Bid.None))
        }
    }

    @Test
    @Order(6)
    fun `when someone plays a card in the first round`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(Card("A")),
            RigsTheDeck.SoThat(sally).willEndUpWith(Card("B")),
            StartsTheGame
        )

        freddy and sally both Bid(1)
        freddy(
            Ensures(TheirHand, !isEmpty),
            PlaysCard("A"),
            Ensures(TheirHand, isEmpty),
        )
        freddy and sally both Ensure(TheCurrentTrick, onlyContains(Card("A").playedBy(freddy)))
    }

    @Test
    @Order(6)
    fun `when everyone has played a card in the first round`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(Card("C")),
            RigsTheDeck.SoThat(sally).willEndUpWith(Card("D")),
            StartsTheGame
        )

        freddy and sally both Bid(1)
        freddy(PlaysCard("C"))
        sally(PlaysCard("D"))
        freddy and sally both Ensure(
            TheCurrentTrick, onlyContains(
                Card("C").playedBy(freddy),
                Card("D").playedBy(sally)
            )
        )

        // TODO: put something here about ending the trick and/or round1
    }
}

private fun Card.playedBy(actor: Actor): PlayedCard = PlayedCard(actor.name, this)

private infix fun Pair<Actor, Actor>.both(activity: Activity) {
    first(activity)
    second(activity)
}

private infix fun Actor.and(other: Actor) = this to other

private fun areOnly(vararg expected: Actor): Matcher<Collection<PlayerId>> =
    areOnly<PlayerId>(*expected.map { it.name }.toTypedArray())

private fun <T> onlyContains(vararg expected: T): Matcher<Collection<T>> = areOnly(*expected)

private fun <T> areOnly(vararg expected: T): Matcher<Collection<T>> =
    object : Matcher<Collection<T>?> {
        override fun invoke(actual: Collection<T>?): MatchResult {
            if (actual?.toSet() != expected.toSet()) return MatchResult.Mismatch("was: ${describe(actual)}")
            return MatchResult.Match
        }

        override val description: String get() = "contains exactly the same items as ${describe(expected.toList())}"
        override val negatedDescription: String get() = "does not $description"
    }

fun <T> sizeIs(expected: Int): Matcher<List<T>> = has(List<T>::size, equalTo(expected))

fun where(vararg bets: Pair<Actor, Bid>): Matcher<Map<PlayerId, Bid>> =
    equalTo<Map<PlayerId, Bid>>(bets.associate { it.first.name to it.second })

infix fun Actor.bid(bid: Bid): Pair<Actor, Bid> = Pair(this, bid)

val NoBids = emptyMap<PlayerId, Int>()
