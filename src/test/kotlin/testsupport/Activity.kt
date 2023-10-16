package testsupport

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue

open class Activity(private val name: String = "AnonymousActivity", private val fn: (Actor) -> Unit) {
    override fun toString(): String  = name
    operator fun invoke(actor: Actor) = fn(actor)
}

inline fun <reified T : Throwable> Activity.expectingFailure() = Activity("Expecting Failure") { actor ->
    withClue("$actor expected activity '$this' to fail") {
        shouldThrow<T> { actor.invoke(this@expectingFailure) }
    }
}
