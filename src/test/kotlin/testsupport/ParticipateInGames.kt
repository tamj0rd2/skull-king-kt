package testsupport

val enterName = Interaction { actor ->
    actor.use<ParticipateInGames>().enterName(actor.name)
}

val joinRoom = Interaction { actor ->
    actor.use<ParticipateInGames>().joinDefaultRoom()
}

fun placeABet(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}

val sitAtTheTable = Task(enterName, joinRoom)
