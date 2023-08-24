import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import testsupport.Activity
import testsupport.Actor
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.PlayACard
import testsupport.ThePlayersAtTheTable
import testsupport.ThePlayersWhoHavePlacedABet
import testsupport.Question
import testsupport.RigTheDeckWith
import testsupport.TheCurrentTrick
import testsupport.TheGamePhase
import testsupport.TheGameState
import testsupport.TheirHand
import testsupport.TheySeeBets
import testsupport.placeABet
import testsupport.sitAtTheTable
import testsupport.StartTheGame
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
        freddy.attemptsTo(
            sitAtTheTable,
            ensureThat(ThePlayersAtTheTable, onlyIncludes(freddy.name)),
            ensureThat(TheGameState, equalTo(GameState.WaitingForMorePlayers)),
        )
    }

    @Test
    @Order(2)
    fun `joining a game when someone else is already waiting to play`() {
        freddy.attemptsTo(sitAtTheTable)
        sally.attemptsTo(sitAtTheTable)

        players.map { actor ->
            actor.attemptsTo(
                ensureThat(ThePlayersAtTheTable, onlyIncludes(freddy.name, sally.name)),
                ensureThat(TheGameState, equalTo(GameState.WaitingToStart))
            )
        }
    }

    @Test
    @Order(3)
    fun `entering the bidding phase`() {
        freddy.attemptsTo(sitAtTheTable)
        sally.attemptsTo(sitAtTheTable)
        gary.attemptsTo(StartTheGame)

        players.forEach { actor ->
            actor.attemptsTo(
                ensureThat(TheGameState, equalTo(GameState.InProgress)),
                ensureThat(TheGamePhase, equalTo(GamePhase.Bidding)),
                ensureThat(TheirHand, hasSize(1)),
            )
        }
    }

    @Test
    @Order(4)
    fun `when everyone has completed their bid`() {
        val bets = mapOf(freddy.name to 1, sally.name to 0)

        players.forEach { it.attemptsTo(sitAtTheTable) }
        gary.attemptsTo(StartTheGame)
        players.forEach { actor -> actor.attemptsTo(placeABet(bets[actor.name]!!)) }
        players.forEach { actor -> actor.attemptsTo(
            ensureThat(TheySeeBets, equalTo(bets)),
            ensureThat(TheGamePhase, equalTo(GamePhase.TrickTaking))
        )}
    }

    @Test
    @Order(5)
    fun `when not everyone has finished bidding`() {
        players.forEach { it.attemptsTo(sitAtTheTable) }
        gary.attemptsTo(StartTheGame)
        freddy.attemptsTo(placeABet(1))

        players.forEach { actor ->
            actor.attemptsTo(
                ensureThat(ThePlayersWhoHavePlacedABet, onlyIncludes(freddy.name)),
                ensureThat(TheGamePhase, equalTo(GamePhase.Bidding)),
                ensureThat(TheySeeBets, equalTo(emptyMap())),
            )
        }
    }

    @Test
    @Order(6)
    fun `playing the first round`() {
        players.forEach { it.attemptsTo(sitAtTheTable) }

        val handsToDeal = mapOf(
            freddy.name to listOf(Card("1")),
            sally.name to listOf(Card("2")),
        )

        fun Map<PlayerId, List<Card>>.playedCard(actor: Actor, index: Int) = PlayedCard(actor.name, this[actor.name]!![index])

        gary.attemptsTo(RigTheDeckWith(handsToDeal))
        gary.attemptsTo(StartTheGame)

        players.forEach { it.attemptsTo(placeABet(1)) }

        freddy.attemptsTo(
            ensureThat(TheirHand, onlyIncludes(Card("1"))),
            PlayACard("1"),
            ensureThat(TheirHand, isEmpty),
        )
        players.forEach { it.attemptsTo(ensureThat(TheCurrentTrick, onlyIncludes(handsToDeal.playedCard(freddy, 0)))) }

        // then freddy and sally can both see the card
    }
}

private fun <T> ensureThat(question: Question<T>, matcher: Matcher<T>) = Activity { actor ->
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
