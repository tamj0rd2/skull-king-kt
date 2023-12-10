package testsupport

import org.slf4j.LoggerFactory

internal val logger = LoggerFactory.getLogger(">> TEST <<")

open class Activity(private val name: String = "AnonymousActivity", private val fn: (Actor) -> Unit) {
    override fun toString(): String  = name
    operator fun invoke(actor: Actor) {
        if (this !is EnsureActivity) logger.info("${actor.name} is attempting to '$this'")
        fn(actor)
    }
}
