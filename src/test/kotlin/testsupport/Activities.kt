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

fun interface Question<T> {
    fun ask(actor: Actor): T
}

val enterName = Interaction { actor ->
    actor.abilities.mustFind<ParticipateInGames>().enterName(actor.name)
}

val joinRoom = Interaction { actor ->
    actor.abilities.mustFind<ParticipateInGames>().joinDefaultRoom()
}

fun placeABet(bet: Int) = Interaction { actor ->
    actor.abilities.mustFind<ParticipateInGames>().placeBet(bet)
}

val sitAtTheTable = Task(enterName, joinRoom)

val startTheGame = Interaction { actor ->
    actor.abilities.mustFind<StartGames>().startGame()
}

inline fun <reified T : Ability> Set<Ability>.mustFind(): T = this.find { it is T }?.let { (it as T) } ?: error("interactor does not possess ability ${T::class.simpleName}")
