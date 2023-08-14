package testsupport

class Actor(val name: String) {
    private val _abilities = mutableSetOf<Ability>()
    val abilities get(): Set<Ability> = _abilities

    fun whoCan(ability: Ability): Actor {
        _abilities += ability
        return this
    }

    fun attemptsTo(vararg activities: Activity) {
        activities.forEach { it.invoke(this) }
    }
}
