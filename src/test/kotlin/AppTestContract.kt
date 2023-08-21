import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasElement
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestMethodOrder
import testsupport.Activity
import testsupport.Actor
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.Question
import testsupport.gameHasStarted
import testsupport.placeABet
import testsupport.playersAtTheTable
import testsupport.seeWhoHasPlacedABet
import testsupport.sitAtTheTable
import testsupport.startTheGame
import testsupport.startTheTrickTakingPhase
import testsupport.theirCardCount
import testsupport.theySeeBets
import testsupport.waitingForMorePlayers
import java.time.Clock
import kotlin.test.Ignore
import kotlin.test.Test

interface Driver : ApplicationDriver, GameMasterDriver

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AppTestContract {
    abstract val participateInGames: () -> ParticipateInGames
    abstract val manageGames: () -> ManageGames

    private val freddy by lazy { Actor("Freddy First").whoCan(participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(participateInGames()) }
    private val gary by lazy { Actor("Gary GameMaster").whoCan(manageGames()) }

    @Test
    @Order(1)
    fun `scenario - joining a game when no one else is waiting`() {
        freddy.attemptsTo(
            sitAtTheTable,
            ensureThat(playersAtTheTable, onlyIncludes(freddy.name)),
            ensureThat(waitingForMorePlayers, isTrue)
        )
    }

    @TestFactory
    @Order(2)
    fun `scenario - joining a game when someone else is already waiting to play`(): List<DynamicTest> {
        freddy.attemptsTo(sitAtTheTable)
        sally.attemptsTo(sitAtTheTable)

        return listOf(freddy, sally).map { actor ->
            DynamicTest.dynamicTest("from ${actor}'s perspective") {
                actor.attemptsTo(
                    ensureThat(playersAtTheTable, onlyIncludes(freddy.name, sally.name)),
                    ensureThat(waitingForMorePlayers, isFalse),
                    ensureThat(gameHasStarted, isFalse),
                )
            }
        }
    }

    @Test
    @Order(3)
    fun `scenario - starting round 1`() {
        freddy.attemptsTo(sitAtTheTable)
        sally.attemptsTo(sitAtTheTable)
        gary.attemptsTo(startTheGame)

        listOf(freddy, sally).forEach { actor ->
            actor.attemptsTo(ensureThat(gameHasStarted, isTrue), ensureThat(theirCardCount, equalTo(1)))
        }
    }

    @Test
    @Order(4)
    fun `scenario - bids are shown after completing bidding`() {
        val players = listOf(freddy, sally)
        val bets = mapOf(freddy.name to 1, sally.name to 0)

        players.forEach { it.attemptsTo(sitAtTheTable) }
        gary.attemptsTo(startTheGame)
        players.forEach { actor -> actor.attemptsTo(placeABet(bets[actor.name]!!)) }
        players.forEach { actor -> actor.attemptsTo(ensureThat(theySeeBets, equalTo(bets))) }
    }

    @Test
    @Order(5)
    fun `scenario - bids are not shown if not everyone has finished bidding`() {
        // TODO: I saw something online about "setting the scene". Maybe I can borrow that...
        val players = listOf(freddy, sally)

        players.forEach { it.attemptsTo(sitAtTheTable) }
        gary.attemptsTo(startTheGame)

        freddy.attemptsTo(placeABet(1))
        players.forEach { actor ->
            actor.attemptsTo(
                ensureThat(seeWhoHasPlacedABet, hasElement(freddy.name)),
                ensureThat(theySeeBets, equalTo(emptyMap()))
            )
        }
    }

    @Test
    @Order(6)
    @Ignore
    // TODO: WIP. ignore this.
    fun `scenario - taking tricks once bidding is complete`() {
        val bets = mapOf(freddy to 0, sally to 1)

        // Given the game master has rigged the deck
        listOf(freddy, sally).forEach { it.attemptsTo(sitAtTheTable) }
        gary.attemptsTo(startTheGame)
        bets.forEach { (actor, bet) -> actor.attemptsTo(placeABet(bet)) }
        gary.attemptsTo(startTheTrickTakingPhase)
        //TODO("write the Then")
    }
}


private fun <T> ensureThat(question: Question<T>, matcher: Matcher<T>) = Activity { abilities ->
    val clock = Clock.systemDefaultZone()
    val startTime = clock.instant()
    val mustEndBy = startTime.plusSeconds(2)

    do {
        try {
            val answer = question.ask(abilities)
            assertThat(answer, matcher)
            break
        } catch (e: AssertionError) {
            if (clock.instant() > mustEndBy) throw e
            Thread.sleep(100)
        }
    } while (true)
}

private fun <T> onlyIncludes(vararg elements: T): Matcher<Collection<T>> = elements.toList()
    .map { it }
    .drop(1)
    .fold(hasElement(elements[0])) { acc, name -> acc.and(hasElement(name)) }
    .and(has(Collection<T>::size, equalTo(elements.size)))

private val isTrue = equalTo(true)
private val isFalse = equalTo(false)
