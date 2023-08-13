package testsupport

fun interface Activity {
    fun invoke(abilities: Set<Ability>)
}

fun interface Interaction: Activity

fun interface Task: Activity

fun interface Question<T> {
    fun ask(abilities: Set<Ability>): T
}

fun enterName(name: String) = Interaction { abilities ->
    abilities.mustFind<AccessTheApplication>().enterName(name)
}

val joinRoom1 = Interaction {abilities ->
    abilities.mustFind<AccessTheApplication>().joinDefaultRoom()
}

fun joinARoom(name: String) = Task { abilities ->
    enterName(name).invoke(abilities)
    joinRoom1.invoke(abilities)
}

inline fun <reified T : Ability> Set<Ability>.mustFind(): T = this.find { it is T }?.let { (it as T) } ?: error("interactor does not possess ability ${T::class.simpleName}")
