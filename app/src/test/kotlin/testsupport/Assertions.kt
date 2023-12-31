package testsupport

import com.tamj0rd2.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.opentest4j.MultipleFailuresError
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.*
import java.lang.management.ManagementFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration

infix fun Activity.wouldFailBecause(errorCode: GameErrorCode) = EnsureActivity { actor ->
    expectCatching { actor.invoke(this@wouldFailBecause) }
        .isFailure()
        .isA<GameErrorCodeException>()
        .get { errorCode }.isEqualTo(errorCode)
}

class EnsureActivity(fn: (Actor) -> Unit) : Activity("assertion", fn)

interface Ensurer {
    fun ensure(timeout: Duration? = null, block: Ensure.() -> Unit): EnsureActivity
    fun ensures(timeout: Duration? = null, block: Ensure.() -> Unit): EnsureActivity = ensure(timeout, block)

    fun <T> ensureThat(question: Question<T>, timeout: Duration? = null, block: Builder<T>.() -> Unit): EnsureActivity
    fun <T> ensuresThat(question: Question<T>, timeout: Duration? = null, block: Builder<T>.() -> Unit): EnsureActivity =
        ensureThat(question, timeout, block)

    operator fun <T> invoke(question: Question<T>, block: Builder<T>.() -> Unit, timeout: Duration? = null) =
        ensureThat(question, timeout, block)
}


interface Ensure {
    fun <T> that(question: Question<T>, timeout: Duration? = null, block: Builder<T>.() -> Unit)
}

// TODO: I can get rid of the Ensurer object can't I? I can just have functions
fun ensurer(): Ensurer {
    return object : Ensurer {
        override fun ensure(timeout: Duration?, block: Ensure.() -> Unit) = EnsureActivity { actor ->
            val capturedWithin = timeout

            object : Ensure {
                override fun <T> that(
                    question: Question<T>,
                    timeout: Duration?,
                    block: Builder<T>.() -> Unit
                ) = actor.invoke(ensureThat(question = question, block = block, timeout = timeout ?: capturedWithin))
            }.apply(block)
        }

        override fun <T> ensureThat(
            question: Question<T>,
            timeout: Duration?,
            block: Builder<T>.() -> Unit,
        ) = EnsureActivity { actor ->
            keepTrying(timeout) {
                try {
                    val answer = question.answeredBy(actor)
                    expectThat(answer).describedAs(question.description).apply(block)
                } catch (e: MultipleFailuresError) {
                    throw AssertionError(e.message)
                }
            }
        }
    }
}

internal infix fun Pair<Actor, Actor>.both(activity: Activity) {
    listOf(first, second).each(activity)
}

val isInDebugMode = ManagementFactory.getRuntimeMXBean().inputArguments.none { it.contains("jdwp") }
internal infix fun List<Actor>.each(activity: Activity) {
    if (activity is EnsureActivity && !isInDebugMode) {
        parallelMap { actor -> actor(activity) }
            .firstOrNull { it.isFailure }
            ?.onFailure { throw Error("activity '$activity' failed", it) }

        return
    }

    forEach { actor -> actor(activity) }
}

private fun <A, B> List<A>.parallelMap(f: suspend (A) -> B): List<Result<B>> = runBlocking {
    map {
        async(Dispatchers.Default) {
            runCatching { f(it) }
        }
    }.map { it.await() }
}

fun <T> keepTrying(timeout: Duration?, initialDelay: Duration? = null, block: () -> T): T {
    initialDelay?.let { Thread.sleep(it.inWholeMilliseconds) }

    val startTime = Instant.now()
    val mustEndBy = startTime.plus((timeout ?: Duration.ZERO).toJavaDuration())

    do {
        try {
            return block()
        } catch (e: AssertionError) {
            if (Instant.now() > mustEndBy) {
                throw e.cleanStackTrace()
            }
            Thread.sleep(50)
        }
    } while (true)
}

private fun Throwable.cleanStackTrace(): Throwable {
    val ignoredClasses = listOf("testsupport.", "org.junit.", "jdk.internal.reflect")
    stackTrace = stackTrace
        .filterNot { ignoredClasses.any { ignored -> it.className.startsWith(ignored) } }
        .toTypedArray()
    return this
}

fun Builder<List<CardWithPlayability>>.onlyContainsCardsThatArePlayable() = this.apply {
    isNotEmpty()
    all { isAPlayableCard() }
}

fun Builder<List<CardWithPlayability>>.onlyContainsCardsThatAreNotPlayable() = this.apply {
    isNotEmpty()
    none { isAPlayableCard() }
}

fun Builder<List<CardWithPlayability>>.containsAtLeast1PlayableCard() = this.apply {
    isNotEmpty()
    atLeast(1) { isAPlayableCard() }
}

fun Builder<CardWithPlayability>.isAPlayableCard() = this.apply {
    get { isPlayable }.isEqualTo(true)
}

fun Builder<List<PlayerId>>.are(vararg actors: Actor) = this.apply {
    containsExactly(actors.map { it.playerId })
}

fun Builder<PlayerId?>.Is(expected: Actor) = this.apply {
    isNotNull().isEqualTo(expected.playerId)
}

fun <T> Builder<T>.Is(expected: T) = this.apply {
    isEqualTo(expected)
}

fun Builder<Map<PlayerId, DisplayBid>>.areFor(first: Pair<PlayerId, DisplayBid>, vararg others: Pair<PlayerId, DisplayBid>) = this.apply {
    isEqualTo(mapOf(first, *others))
}

fun Builder<Map<PlayerId, Int>>.are(first: Pair<PlayerId, Int>, vararg others: Pair<PlayerId, Int>) = this.apply {
    isEqualTo(mapOf(first, *others))
}

infix fun <T : Collection<E>, E> Builder<T>.sizeIs(expected: Int): Builder<T> = this.apply { hasSize(expected) }

fun Builder<List<PlayedCard>?>.cardsAre(firstCard: PlayedCard, vararg others: PlayedCard) = this.apply {
    isNotNull().containsExactly(firstCard, *others)
}
