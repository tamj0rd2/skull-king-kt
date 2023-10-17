package testsupport

import com.tamj0rd2.domain.PlayerId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val defaultDelay = 1.seconds

fun interface Assertion<T> {
    fun T?.assert()
}

fun <T> waitUntil(question: Question<T>, assertion: Assertion<T>, within: Duration) = ensure(question, assertion, within)
fun <T> Is(expected: T) = Assertion<T> { this shouldBe expected }
fun <T> sizeIs(expected: Int) = Assertion<List<T>> { withClue("checking size") { this?.size shouldBe expected } }
fun <T> onlyContains(vararg expected: T) = Assertion<List<T>> { this shouldBe expected.toList() }
fun <T> isEmpty() = Assertion<List<T>> { this shouldBe emptyList() }
fun are(vararg expected: Actor) = Assertion<List<PlayerId>> { this shouldBe expected.map { it.playerId }.toList() }
fun <T> where(vararg data: Pair<Actor, T>) = Assertion<Map<PlayerId, T>> { this shouldBe data.associate { it.first.playerId to it.second } }

interface Ensure {
    fun <T> that(question: Question<T>, assertion: Assertion<T>, within: Duration? = null)
    fun <T> Is(expected: T) = testsupport.Is(expected)
    fun are(vararg expected: Actor) = testsupport.are(*expected)
    fun <T> where(vararg data: Pair<Actor, T>) = testsupport.where(*data)
    fun <T> sizeIs(expected: Int) = testsupport.sizeIs<T>(expected)
    fun <T> onlyContains(vararg expected: T) = testsupport.onlyContains(*expected)
    fun <T> isEmpty() = testsupport.isEmpty<T>()
}

class EnsureActivity(fn: (Actor) -> Unit) : Activity("assertion", fn)

fun ensure(within: Duration = defaultDelay, block: Ensure.() -> Unit) = EnsureActivity { actor ->
    val outerWithin = within

    object : Ensure {
        override fun <T> that(question: Question<T>, assertion: Assertion<T>, within: Duration?) {
            actor.invoke(
                ensure(
                    question = question,
                    assertion = assertion,
                    within = within ?: outerWithin
                )
            )
        }
    }.apply(block)
}

fun ensures(within: Duration = defaultDelay, block: Ensure.() -> Unit) = ensure(within, block)
fun <T> ensure(question: Question<T>, assertion: Assertion<T>, within: Duration = defaultDelay) = EnsureActivity { actor ->
    val mustEndBy = Instant.now().plus(within.toJavaDuration())

    do {
        try {
            val answer = question.answeredBy(actor)
            withClue("$actor asked a $question") {
                // this is some voodoo magic right here
                // I think the way it works is that `with` makes all the extensions available, therefore we're
                // now able to apply the assert extension to the answer. madness.
                with(assertion) { answer.assert() }
            }
            break
        } catch (e: AssertionError) {
            if (Instant.now() > mustEndBy) {
                val ignoredClasses = listOf("testsupport.", "org.junit.", "jdk.internal.reflect")

                e.stackTrace = e.stackTrace
                    .filterNot { ignoredClasses.any { ignored -> it.className.startsWith(ignored) } }
                    .toTypedArray()
                throw e
            }
            Thread.sleep(50)
        }
    } while (true)
}

fun <T> ensures(question: Question<T>, assertion: Assertion<T>, within: Duration = defaultDelay) =
    ensure(question, assertion, within)