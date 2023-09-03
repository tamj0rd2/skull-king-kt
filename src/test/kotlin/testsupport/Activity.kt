package testsupport

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws

fun interface Activity {
    operator fun invoke(actor: Actor)
}

inline fun <reified T : Throwable> Activity.expectingFailure() = Activity { actor ->
    assertThat({ actor.invoke(this) }, throws<T>())
}

fun interface Interaction: Activity

class Task(private vararg val activities: Activity): Activity {
    override fun invoke(actor: Actor) {
        actor.invoke(*activities)
    }
}
