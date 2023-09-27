package testsupport

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import java.time.Instant.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

open class Question<T>(private val description: String, private val answer: (Actor) -> T) {
    fun answeredBy(actor: Actor): T = answer(actor)
    override fun toString(): String  = "question about $description"
    companion object {
        fun <R> about(description: String, answer: (Actor) -> R) = object : Question<R>(description, answer) {}
    }
}

val ThePlayersAtTheTable = Question.about("the players at the table") { actor ->
    actor.use<ParticipateInGames>().playersInRoom
}

// NOTE: these could all be rolled up into a single question about the game state...
val TheRoundNumber = Question.about("the round number") { actor ->
    actor.use<ParticipateInGames>().roundNumber
}

val TheTrickNumber = Question.about("the trick number") { actor ->
    actor.use<ParticipateInGames>().trickNumber
}

val TheGameState = Question.about("the game state") { actor ->
    actor.use<ParticipateInGames>().gameState
}

val TheRoundPhase = Question.about("the round phase") { actor ->
    actor.use<ParticipateInGames>().roundPhase
}

val TheCurrentPlayer = Question.about("the current player") { actor ->
    actor.use<ParticipateInGames>().currentPlayer
}

val TheWinnerOfTheTrick = Question.about("the winner of the trick") { actor ->
    actor.use<ParticipateInGames>().trickWinner
}

val TheirHand = Question.about("their hand") { actor ->
    actor.use<ParticipateInGames>().hand
}
val HisHand = TheirHand
val HerHand = TheirHand

val TheirFirstCard = Question.about("the first card in their hand") { actor ->
    actor.use<ParticipateInGames>().hand.first()
}
val HisFirstCard = TheirFirstCard
val HerFirstCard = TheirFirstCard

val TheySeeBids = Question.about("the bids that have been made") { actor ->
    actor.use<ParticipateInGames>().bids
}

val TheySeeWinsOfTheRound = Question.about("the actual wins during the round") { actor ->
    actor.use<ParticipateInGames>().winsOfTheRound
}

val TheCurrentTrick = Question.about("the current trick") { actor ->
    actor.use<ParticipateInGames>().trick
}

private val defaultDelay = 1.seconds

fun <T> waitUntil(question: Question<T>, matcher: Matcher<T>, within: Duration = defaultDelay) = ensure(question, matcher, within)

interface Ensure {
    fun <T> that(question: Question<T>, matcher: Matcher<T>, within: Duration? = null)
    fun <T> Is(expected: T?): Matcher<T>
}

fun ensure(within: Duration = defaultDelay, block: Ensure.() -> Unit) = Activity { actor ->
    val outerWithin = within

    object : Ensure {
        override fun <T> that(question: Question<T>, matcher: Matcher<T>, within: Duration?) {
            actor.invoke(ensure(
                question = question,
                matcher = matcher,
                within = within ?: outerWithin
            ))
        }

        override fun <T> Is(expected: T?): Matcher<T> = equalTo(expected)
    }.apply(block)
}
fun ensures(within: Duration = defaultDelay, block: Ensure.() -> Unit) = ensure(within, block)

fun <T> ensure(question: Question<T>, matcher: Matcher<T>, within: Duration = defaultDelay) = Activity { actor ->
    val mustEndBy = now().plus(within.toJavaDuration())

    do {
        try {
            val answer = question.answeredBy(actor)
            assertThat(answer, matcher) { "$actor asked a $question" }
            break
        } catch (e: AssertionError) {
            if (now() > mustEndBy) throw e
            Thread.sleep(50)
        }
    } while (true)
}

fun <T> ensures(question: Question<T>, matcher: Matcher<T>, within: Duration = defaultDelay) = ensure(question, matcher, within)
