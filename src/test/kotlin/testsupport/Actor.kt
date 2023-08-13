package testsupport

class Actor(val name: String) {
    private val abilities = mutableSetOf<Ability>()

    fun can(ability: Ability): Actor {
        abilities += ability
        return this
    }

    fun attemptsTo(vararg activities: Activity) {
        activities.forEach { it.invoke(abilities) }
    }
}
