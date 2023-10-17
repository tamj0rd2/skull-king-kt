package testsupport

import com.tamj0rd2.domain.PlayerId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun interface Assertion<T> {
    fun T?.assert()
}

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

interface Ensurer {
    fun ensure(block: Ensure.() -> Unit): EnsureActivity
    fun ensures(block: Ensure.() -> Unit) = ensure(block)
    operator fun invoke(block: Ensure.() -> Unit) = ensure(block)

    fun ensure(within: Duration, block: Ensure.() -> Unit): EnsureActivity
    fun ensures(within: Duration, block: Ensure.() -> Unit) = ensure(within, block)
    operator fun invoke(within: Duration, block: Ensure.() -> Unit) = ensure(within, block)

    fun <T> ensure(question: Question<T>, assertion: Assertion<T>, within: Duration? = null): EnsureActivity
    fun <T> ensures(question: Question<T>, assertion: Assertion<T>, within: Duration? = null) = ensure(question, assertion, within)
    operator fun <T> invoke(question: Question<T>, assertion: Assertion<T>, within: Duration? = null) = ensure(question, assertion, within)
}

fun ensurer(within: Duration): Ensurer {
    val outerWithin = within
    return object : Ensurer {
        override fun ensure(block: Ensure.() -> Unit) = EnsureActivity { actor ->
            object : Ensure {
                override fun <T> that(question: Question<T>, assertion: Assertion<T>, within: Duration?) {
                    actor.invoke(
                        ensure(
                            question = question,
                            assertion = assertion,
                            within = within
                        )
                    )
                }
            }.apply(block)
        }

        override fun ensure(within: Duration, block: Ensure.() -> Unit) = EnsureActivity { actor ->
            val middleWithin = within
            object : Ensure {
                override fun <T> that(question: Question<T>, assertion: Assertion<T>, within: Duration?) {
                    actor.invoke(
                        ensure(
                            question = question,
                            assertion = assertion,
                            within = within ?: middleWithin
                        )
                    )
                }
            }.apply(block)
        }

        override fun <T> ensure(question: Question<T>, assertion: Assertion<T>, within: Duration?) = EnsureActivity { actor ->
            val mustEndBy = Instant.now().plus((within ?: outerWithin).toJavaDuration())

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
    }
}
