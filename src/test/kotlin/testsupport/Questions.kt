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

val theirCardCount = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().getCardCount(actor.name)
}

val theySeeBets = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().getBets()
}

val seeWhoHasPlacedABet = Question { actor ->
    actor.abilities.mustFind<ParticipateInGames>().getPlayersWhoHavePlacedBets()
}

