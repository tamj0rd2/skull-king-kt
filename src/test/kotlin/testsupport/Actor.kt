package testsupport

class Actor(val name: String) {
    private val _abilities = mutableSetOf<Ability>()
    val abilities get(): Set<Ability> = _abilities

    fun whoCan(vararg abilities: Ability): Actor {
        _abilities += abilities
        return this
    }

    operator fun invoke(vararg activities: Activity) {
        activities.forEach { it.invoke(this) }
    }

    override fun toString(): String {
        return name
    }

    inline fun <reified T> use(): T where T : Ability {
        return this.abilities.find { it is T }?.let { (it as T) } ?: error("actor does not have ability ${T::class.simpleName}")
    }
}
