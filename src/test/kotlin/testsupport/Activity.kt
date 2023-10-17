package testsupport

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Activity")

open class Activity(private val name: String = "AnonymousActivity", private val fn: (Actor) -> Unit) {
    override fun toString(): String  = name
    operator fun invoke(actor: Actor) {
        if (this !is EnsureActivity) logger.info("${actor.name} is attempting to '$this'")
        fn(actor)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(">>> TEST <<<")
    }
}

inline fun <reified T : Throwable> Activity.expectingFailure() = Activity("Expecting Failure") { actor ->
    withClue("$actor expected activity '$this' to fail") {
        shouldThrow<T> { actor.invoke(this@expectingFailure) }
    }
}
