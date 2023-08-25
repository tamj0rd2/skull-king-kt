import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GamePhase
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import testsupport.Activity
import testsupport.Actor
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.PlaysACard
import testsupport.ThePlayersAtTheTable
import testsupport.ThePlayersWhoHavePlacedABet
import testsupport.Question
import testsupport.RigsTheDeck
import testsupport.TheCurrentTrick
import testsupport.TheGamePhase
import testsupport.TheGameState
import testsupport.TheirHand
import testsupport.TheySeeBets
import testsupport.PlacesABet
import testsupport.SitsAtTheTable
import testsupport.StartsTheGame
import java.time.Clock
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AppTestContract {
    abstract val participateInGames: () -> ParticipateInGames
    abstract val manageGames: () -> ManageGames

    private val freddy by lazy { Actor("Freddy First").whoCan(participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(participateInGames()) }
    private val gary by lazy { Actor("Gary GameMaster").whoCan(manageGames()) }
    private val players get() = listOf(freddy, sally)

    @Test
    @Order(1)
    fun `joining a game when no one else is waiting`() {
        freddy(
            SitsAtTheTable,
            ensuresThat(ThePlayersAtTheTable, onlyIncludes(freddy.name)),
            ensuresThat(TheGameState, equalTo(GameState.WaitingForMorePlayers)),
        )
    }

    @Test
    @Order(2)
    fun `joining a game when someone else is already waiting to play`() {
        freddy(SitsAtTheTable)
        sally(SitsAtTheTable)

        players.map { actor ->
            actor(
                ensuresThat(ThePlayersAtTheTable, onlyIncludes(freddy.name, sally.name)),
                ensuresThat(TheGameState, equalTo(GameState.WaitingToStart))
            )
        }
    }

    @Test
    @Order(3)
    fun `entering the bidding phase`() {
        freddy(SitsAtTheTable)
        sally(SitsAtTheTable)
        gary(StartsTheGame)

        players.forEach { actor ->
            actor(
                ensuresThat(TheGameState, equalTo(GameState.InProgress)),
                ensuresThat(TheGamePhase, equalTo(GamePhase.Bidding)),
                ensuresThat(TheirHand, hasSize(1)),
            )
        }
    }

    @Test
    @Order(4)
    fun `when everyone has completed their bid`() {
        val bets = mapOf(freddy.name to 1, sally.name to 0)

        players.forEach { it(SitsAtTheTable) }
        gary(StartsTheGame)
        players.forEach { actor -> actor(PlacesABet(bets[actor.name]!!)) }
        players.forEach { actor -> actor(
            ensuresThat(TheySeeBets, equalTo(bets)),
            ensuresThat(TheGamePhase, equalTo(GamePhase.TrickTaking))
        )}
    }

    @Test
    @Order(5)
    fun `when not everyone has finished bidding`() {
        players.forEach { it(SitsAtTheTable) }
        gary(StartsTheGame)
        freddy(PlacesABet(1))

        players.forEach { actor ->
            actor(
                ensuresThat(ThePlayersWhoHavePlacedABet, onlyIncludes(freddy.name)),
                ensuresThat(TheGamePhase, equalTo(GamePhase.Bidding)),
                ensuresThat(TheySeeBets, equalTo(emptyMap())),
            )
        }
    }

    @Test
    @Order(6)
    fun `playing the first round`() {
        players.forEach { it(SitsAtTheTable) }

        val handsToDeal = mapOf(
            freddy.name to listOf(Card("1")),
            sally.name to listOf(Card("2")),
        )

        fun Map<PlayerId, List<Card>>.playedCard(actor: Actor, index: Int) = PlayedCard(actor.name, this[actor.name]!![index])

        gary(RigsTheDeck(handsToDeal))
        gary(StartsTheGame)

        players.forEach { it(PlacesABet(1)) }

        freddy(
            ensuresThat(TheirHand, onlyIncludes(Card("1"))),
            PlaysACard("1"),
            ensuresThat(TheirHand, isEmpty),
        )
        players.forEach { it(ensuresThat(TheCurrentTrick, onlyIncludes(handsToDeal.playedCard(freddy, 0)))) }

        // then freddy and sally can both see the card
    }
}

private fun <T> ensuresThat(question: Question<T>, matcher: Matcher<T>) = Activity { actor ->
    val clock = Clock.systemDefaultZone()
    val startTime = clock.instant()
    val mustEndBy = startTime.plusSeconds(2)

    do {
        try {
            val answer = question.ask(actor)
            assertThat(answer, matcher) { "$actor asked a $question" }
            break
        } catch (e: AssertionError) {
            if (clock.instant() > mustEndBy) throw e
            Thread.sleep(100)
        }
    } while (true)
}

private fun <T> onlyIncludes(vararg expected: T): Matcher<Collection<T>> =
    object : Matcher<Collection<T>?> {
        override fun invoke(actual: Collection<T>?): MatchResult {
            if (actual?.toSet() != expected.toSet()) return MatchResult.Mismatch("was: ${describe(actual)}")
            return MatchResult.Match
        }

        override val description: String get() = "contains exactly the same items as ${describe(expected.toList())}"
        override val negatedDescription: String get() = "does not $description"
    }

fun <T> hasSize(expected: Int): Matcher<List<T>> = has(List<T>::size, equalTo(expected))
