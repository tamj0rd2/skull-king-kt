package testsupport

class Actor(val name: String) {
    private val _abilities = mutableSetOf<Ability>()
    val abilities get(): Set<Ability> = _abilities

    fun whoCan(vararg abilities: Ability): Actor {
        _abilities += abilities
        return this
    }

    fun attemptsTo(vararg activities: Activity) {
        activities.forEach { it.invoke(this) }
    }

    override fun toString(): String {
        return name
    }
}
