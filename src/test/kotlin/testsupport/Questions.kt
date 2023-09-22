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

val TheCurrentTrick = Question.about("the current trick") { actor ->
    actor.use<ParticipateInGames>().trick
}

fun interface EnsureActivity : Activity

val Ensures = Ensure
object Ensure {
    private val defDelay = 1.seconds

    interface That {
        fun <T> that(question: Question<T>, matcher: Matcher<T>, within: Duration? = null)
        fun <T> Is(expected: T?): Matcher<T>
    }

    operator fun invoke(within: Duration = defDelay, block: That.() -> Unit) = EnsureActivity { actor ->
        val outerWithin = within

        object : That {
            override fun <T> that(question: Question<T>, matcher: Matcher<T>, within: Duration?) {
                actor.invoke(Ensure(
                    question = question,
                    matcher = matcher,
                    within = within ?: outerWithin
                ))
            }

            override fun <T> Is(expected: T?): Matcher<T> = equalTo(expected)
        }.apply(block)
    }

    operator fun <T> invoke(question: Question<T>, matcher: Matcher<T>, within: Duration = defDelay) = EnsureActivity { actor ->
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
}
