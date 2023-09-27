package testsupport

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.time.Duration

inline fun <reified E : Throwable> retryEx(total: Duration, backoffMs: Int = 0, block: () -> Unit) {
    val endTime = Instant.now().plusMillis(total.inWholeMilliseconds)
    var lastError: Throwable?

    do {
        try {
            return block()
        } catch (e: Exception) {
            if (e !is E) throw e
            lastError = e
            Thread.sleep(backoffMs.toLong())
        }
    } while (Instant.now() < endTime)

    throw lastError!!
}

fun <T> retry(
    total: Duration,
    backoffMs: Int = 0,
    only: List<KClass<*>> = emptyList(),
    block: () -> T,
): T {
    val endTime = Instant.now().plusMillis(total.inWholeMilliseconds)
    var lastError: Throwable?

    do {
        try {
            return block()
        } catch (e: Exception) {
            if (only.isNotEmpty() && only.none { it.isInstance(e) }) {
                throw e
            }

            lastError = e
            Thread.sleep(backoffMs.toLong())
        }
    } while (Instant.now() < endTime)

    throw lastError!!
}