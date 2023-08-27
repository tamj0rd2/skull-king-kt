import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Bid.*
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
import testsupport.Bids
import testsupport.Ensure
import testsupport.Ensures
import testsupport.HisHand
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.Play
import testsupport.Plays
import testsupport.RigsTheDeck
import testsupport.SaysTheGameCanStart
import testsupport.SaysTheNextRoundCanStart
import testsupport.SaysTheNextTrickCanStart
import testsupport.SitAtTheTable
import testsupport.SitsAtTheTable
import testsupport.TheCurrentTrick
import testsupport.TheGamePhase
import testsupport.TheGameState
import testsupport.ThePlayersAtTheTable
import testsupport.TheRoundNumber
import testsupport.TheTrickNumber
import testsupport.TheirHand
import testsupport.TheySeeBids
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import com.natpryce.hamkrest.equalTo as Is

interface AbilityFactory {
    fun participateInGames(): ParticipateInGames
    fun manageGames(): ManageGames
}

interface TestConfiguration : AbilityFactory {
    fun setup()
    fun teardown()
}

// things for the future:
// turn order
// scoring
// using actual cards lol

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
    fun `starting a game`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy and sally both Ensure {
            that(TheGameState, Is(InProgress))
            that(TheGamePhase, Is(Bidding))
            that(HisHand, sizeIs(1))
        }
    }

    @Test
    @Order(4)
    fun `when everyone has bid`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy(Bids(1))
        sally(Bids(2))
        freddy and sally both Ensure {
            that(TheySeeBids, where(freddy bid 1, sally bid 2))
            that(TheGamePhase, Is(TrickTaking))
        }
    }

    @Test
    @Order(5)
    fun `when someone hasn't bid`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy(Bids(1))
        freddy and sally both Ensure {
            that(TheGamePhase, Is(Bidding))
            that(TheySeeBids, where(freddy.bidIsHidden(), sally.hasNotBid()))
        }
    }

    @Test
    @Order(6)
    fun `when someone plays a card in the first round`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(Card("A")),
            RigsTheDeck.SoThat(sally).willEndUpWith(Card("B")),
            SaysTheGameCanStart
        )

        freddy and sally both Bid(1)
        freddy(
            Ensures(HisHand, sizeIs(1)),
            Plays.card("A"),
            Ensures(HisHand, isEmpty),
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
            SaysTheGameCanStart
        )

        freddy and sally both Bid(1)
        freddy(Plays.card("C"))
        sally(Plays.card("D"))
        freddy and sally both Ensure(
            TheCurrentTrick, onlyContains(
                Card("C").playedBy(freddy),
                Card("D").playedBy(sally)
            )
        )

        // TODO: put something here about ending the trick and/or round1
    }


    @Test
    @Order(7)
    fun `playing a game from start to finish`() {
        freddy and sally both SitAtTheTable

        // round 1
        gary(SaysTheGameCanStart)
        freddy and sally both Ensure {
            that(TheRoundNumber, Is(1))
            that(TheTrickNumber, Is(1))
            that(TheirHand, sizeIs(1))
        }

        // round 1 bidding
        freddy and sally both Bid(1)
        freddy and sally both Ensure {
            that(TheySeeBids, where(freddy bid 1, sally bid 1))
            that(TheGamePhase, Is(TrickTaking))
        }

        // round 1 trick taking
        freddy(Plays.card("1"))
        sally(Plays.card("2"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("1").playedBy(freddy), Card("2").playedBy(sally)))
            that(TheGamePhase, Is(TrickComplete))
        }

        // round 2
        gary(SaysTheNextRoundCanStart)
        freddy and sally both Ensure {
            that(TheRoundNumber, Is(2))
            that(TheTrickNumber, Is(1))
            that(TheirHand, sizeIs(2))
        }

        // round 2 bidding
        freddy and sally both Bid(2)
        freddy and sally both Ensure {
            that(TheySeeBids, where(freddy bid 2, sally bid 2))
            that(TheGamePhase, Is(TrickTaking))
        }

        // round 2 trick 1
        freddy(Plays.card("1"))
        sally(Plays.card("3"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("1").playedBy(freddy), Card("3").playedBy(sally)))
            that(TheGamePhase, Is(TrickComplete))
        }

        // round 2 trick 2
        gary(SaysTheNextTrickCanStart)
        freddy and sally both Ensure(TheTrickNumber, Is(2))
        freddy(Plays.card("2"))
        sally(Plays.card("4"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("2").playedBy(freddy), Card("4").playedBy(sally)))
            that(TheGamePhase, Is(TrickComplete))
        }

        // rounds 3 - 10
        (3..10).forEach { roundNumber ->
            gary(SaysTheNextRoundCanStart)
            freddy and sally both Ensure {
                that(TheRoundNumber, Is(roundNumber))
                that(TheTrickNumber, Is(1))
                that(TheirHand, sizeIs(roundNumber))
            }

            // round X trick 1
            freddy and sally both Play.theirFirstCard
            freddy and sally both Ensure {
                that(TheCurrentTrick, sizeIs(2))
                that(TheGamePhase, Is(TrickComplete))
            }

            // round X trick 2-X
            (2..roundNumber).forEach { trickNumber ->
                gary(SaysTheNextTrickCanStart)
                freddy and sally both Ensure {
                    that(TheRoundNumber, Is(roundNumber))
                    that(TheTrickNumber, Is(trickNumber))
                    that(TheirHand, sizeIs(roundNumber - trickNumber + 1))
                }

                freddy and sally both Play.theirFirstCard
                freddy and sally both Ensure {
                    that(TheCurrentTrick, sizeIs(2))
                    that(TheGamePhase, Is(TrickComplete))
                }
            }
        }

        freddy and sally both Ensure {
            that(TheRoundNumber, Is(10))
            that(TheTrickNumber, Is(10))
            that(TheirHand, isEmpty)
            that(TheGameState, Is(Complete))
        }
    }
}

private fun Card.playedBy(actor: Actor): PlayedCard = this.playedBy(actor.name)

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

// TODO: this can use Collection
fun <T> sizeIs(expected: Int): Matcher<List<T>> = has(List<T>::size, equalTo(expected))

fun where(vararg bets: Pair<Actor, Bid>): Matcher<Map<PlayerId, Bid>> =
    equalTo(bets.associate { it.first.name to it.second })

infix fun Actor.bid(bid: Int): Pair<Actor, Bid> = Pair(this, Placed(bid))
fun Actor.bidIsHidden(): Pair<Actor, Bid> = Pair(this, IsHidden)
fun Actor.hasNotBid(): Pair<Actor, Bid> = Pair(this, None)
