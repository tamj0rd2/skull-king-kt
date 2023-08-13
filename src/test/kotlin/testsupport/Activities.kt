package testsupport

fun interface Activity {
    fun invoke(actor: Actor)
}

fun interface Interaction: Activity

fun interface Task: Activity

fun interface Question<T> {
    fun ask(actor: Actor): T
}

fun enterName(name: String) = Interaction { actor ->
    actor.abilities.mustFind<AccessTheApplication>().enterName(name)
}

val joinRoom1 = Interaction {actor ->
    actor.abilities.mustFind<AccessTheApplication>().joinDefaultRoom()
}

val sitAtTheTable = Task { actor ->
    enterName(actor.name).invoke(actor)
    joinRoom1.invoke(actor)
}

inline fun <reified T : Ability> Set<Ability>.mustFind(): T = this.find { it is T }?.let { (it as T) } ?: error("interactor does not possess ability ${T::class.simpleName}")
