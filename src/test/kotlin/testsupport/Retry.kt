package testsupport

import java.time.Instant
import kotlin.time.Duration

fun <T> retry(total: Duration, backoffMs: Int = 0, block: () -> T): T {
    val endTime = Instant.now().plusMillis(total.inWholeMilliseconds)
    var lastError: Throwable?

    do {
        try {
            return block()
        } catch (e: Throwable) {
            lastError = e
        }
    } while (Instant.now() < endTime)

    throw lastError!!
}