package testsupport

fun waitingForMorePlayers() = Question { abilities ->
    abilities.mustFind<AccessTheApplication>().isWaitingForMorePlayers()
}

fun playersInRoom() = Question { abilities ->
    abilities.mustFind<AccessTheApplication>().getPlayersInRoom()
}
