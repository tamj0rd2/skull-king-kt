package testsupport

val waitingForMorePlayers = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().isWaitingForMorePlayers()
}

val playersAtTheTable = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().getPlayersInRoom()
}

val hasGameStarted = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().hasGameStarted()
}
