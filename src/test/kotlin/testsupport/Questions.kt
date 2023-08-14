package testsupport

val waitingForMorePlayers = Question { actor ->
    actor.abilities.mustFind<AccessTheApplication>().isWaitingForMorePlayers()
}

val playersAtTheTable = Question { actor ->
    actor.abilities.mustFind<AccessTheApplication>().getPlayersInRoom()
}

val hasGameStarted = Question { actor ->
    actor.abilities.mustFind<AccessTheApplication>().hasGameStarted()
}
