package testsupport

fun interface Activity {
    fun invoke(actor: Actor)
}

fun interface Interaction: Activity

class Task(private vararg val activities: Activity): Activity {
    override fun invoke(actor: Actor) {
        activities.forEach { it.invoke(actor) }
    }
}
