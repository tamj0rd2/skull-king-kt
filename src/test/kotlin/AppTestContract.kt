import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Bid.*
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.RoundPhase.*
import com.tamj0rd2.domain.GameState.*
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import org.junit.jupiter.api.assertThrows
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
import testsupport.TheBiddingError
import testsupport.TheRoundPhase
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

sealed class AppTestContract(private val d: TestConfiguration) {
    private val freddy by lazy { Actor("Freddy First").whoCan(d.participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(d.participateInGames()) }
    private val gary by lazy { Actor("Gary GameMaster").whoCan(d.manageGames()) }

    @BeforeTest fun setup() = d.setup()

    @AfterTest fun teardown() = d.teardown()

    @Test
    fun `sitting at an empty table and waiting for more players to join`() {
        freddy(
            SitsAtTheTable,
            Ensures {
                that(ThePlayersAtTheTable, areOnly(freddy))
                that(TheGameState, Is(WaitingForMorePlayers))
            },
        )
    }

    @Test
    fun `waiting for sally to bid`() {
        freddy and sally both SitAtTheTable
        gary(SaysTheGameCanStart)
        freddy(Bids(1))
        freddy and sally both Ensure {
            that(TheRoundPhase, Is(Bidding))
            that(TheySeeBids, where(freddy.bidIsHidden(), sally.hasNotBid()))
        }
    }

    @Test
    fun `playing a card and waiting for the next player to do the same`() {
        freddy and sally both SitAtTheTable
        gary(
            RigsTheDeck.SoThat(freddy).willEndUpWith(Card("A")),
            SaysTheGameCanStart
        )
        freddy and sally both Bid(1)

        gary(SaysTheNextTrickCanStart)
        freddy(
            Ensures(HisHand, sizeIs(1)),
            Plays.card("A"),
            Ensures(HisHand, isEmpty),
        )

        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("A").playedBy(freddy)))
            that(TheRoundPhase, Is(TrickTaking))
        }
    }

    @Test
    fun `playing a game from start to finish`() {
        freddy and sally both SitAtTheTable
        freddy and sally both Ensure {
            that(ThePlayersAtTheTable, areOnly(freddy, sally))
            that(TheGameState, Is(WaitingToStart))
        }

        // round 1
        gary(SaysTheGameCanStart)
        freddy and sally both Ensure {
            that(TheRoundNumber, Is(1))
            that(TheirHand, sizeIs(1))
            that(TheRoundPhase, Is(Bidding))
        }

        // round 1 bidding
        freddy and sally both Bid(1)
        freddy and sally both Ensure {
            that(TheySeeBids, where(freddy bid 1, sally bid 1))
            that(TheRoundPhase, Is(TrickTaking))
        }

        // round 1 trick taking
        gary(SaysTheNextTrickCanStart)
        freddy and sally both Ensure(TheTrickNumber, Is(1))
        freddy(Plays.card("1"))
        sally(Plays.card("2"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("1").playedBy(freddy), Card("2").playedBy(sally)))
            that(TheRoundPhase, Is(TrickComplete))
        }

        // round 2
        gary(SaysTheNextRoundCanStart)
        freddy and sally both Ensure {
            that(TheRoundNumber, Is(2))
            that(TheirHand, sizeIs(2))
            that(TheRoundPhase, Is(Bidding))
        }

        // round 2 bidding
        freddy and sally both Bid(2)
        freddy and sally both Ensure {
            that(TheySeeBids, where(freddy bid 2, sally bid 2))
            that(TheRoundPhase, Is(TrickTaking))
        }

        // round 2 trick 1
        gary(SaysTheNextTrickCanStart)
        freddy and sally both Ensure(TheTrickNumber, Is(1))
        freddy(Plays.card("1"))
        sally(Plays.card("3"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("1").playedBy(freddy), Card("3").playedBy(sally)))
            that(TheRoundPhase, Is(TrickComplete))
            that(TheirHand, sizeIs(1))
        }

        // round 2 trick 2
        gary(SaysTheNextTrickCanStart)
        freddy and sally both Ensure(TheTrickNumber, Is(2))
        freddy(Plays.card("2"))
        sally(Plays.card("4"))
        freddy and sally both Ensure {
            that(TheCurrentTrick, onlyContains(Card("2").playedBy(freddy), Card("4").playedBy(sally)))
            that(TheRoundPhase, Is(TrickComplete))
            that(TheirHand, isEmpty)
        }

        // rounds 3 - 10
        (3..10).forEach { roundNumber ->
            // round X
            gary(SaysTheNextRoundCanStart)
            freddy and sally both Ensure {
                that(TheRoundNumber, Is(roundNumber))
                that(TheirHand, sizeIs(roundNumber))
            }

            // round X bidding
            freddy and sally both Bid(roundNumber)
            freddy and sally both Ensure {
                that(TheySeeBids, where(freddy bid roundNumber, sally bid roundNumber))
                that(TheRoundPhase, Is(TrickTaking))
            }

            // round X trick 1-X
            (1..roundNumber).forEach { trickNumber ->
                gary(SaysTheNextTrickCanStart)
                freddy and sally both Ensure {
                    that(TheTrickNumber, Is(trickNumber))
                    that(TheirHand, sizeIs(roundNumber - trickNumber + 1))
                }

                freddy and sally both Play.theFirstCardInTheirHand
                freddy and sally both Ensure {
                    that(TheCurrentTrick, sizeIs(2))
                    that(TheRoundPhase, Is(TrickComplete))
                    that(TheirHand, sizeIs(roundNumber - trickNumber))
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

    @Test
    fun `cannot bid before the game has started`() {
        freddy and sally both SitAtTheTable
        freddy(Bids(1))
        freddy(Ensures(TheBiddingError, Is(GameErrorCode.NotStarted)))
        freddy and sally both Ensure(TheGameState, Is(WaitingToStart))
    }

    @Test
    fun `a player can't join twice`() {
        freddy(SitsAtTheTable)
        val freddyOnASecondDevice = Actor(freddy.name).whoCan(d.participateInGames())
        assertThrows<GameException.PlayerWithSameNameAlreadyJoined> { freddyOnASecondDevice(SitsAtTheTable) }
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

fun <T> sizeIs(expected: Int): Matcher<Collection<T>> = has(Collection<T>::size, equalTo(expected))

fun where(vararg bets: Pair<Actor, Bid>): Matcher<Map<PlayerId, Bid>> =
    equalTo(bets.associate { it.first.name to it.second })

infix fun Actor.bid(bid: Int): Pair<Actor, Bid> = Pair(this, Placed(bid))
fun Actor.bidIsHidden(): Pair<Actor, Bid> = Pair(this, IsHidden)
fun Actor.hasNotBid(): Pair<Actor, Bid> = Pair(this, None)
