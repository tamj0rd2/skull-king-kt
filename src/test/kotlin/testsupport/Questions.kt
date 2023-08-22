package testsupport

fun interface Question<T> {
    fun ask(actor: Actor): T
}

val waitingForMorePlayers = Question { actor ->
    actor.use<ParticipateInGames>().isWaitingForMorePlayers
}

val playersAtTheTable = Question { actor ->
    actor.use<ParticipateInGames>().playersInRoom
}

val gameHasStarted = Question { actor ->
    actor.use<ParticipateInGames>().hasGameStarted
}

val theirCardCount = Question { actor ->
    actor.use<ParticipateInGames>().cardCount
}

val theySeeBets = Question { actor ->
    actor.use<ParticipateInGames>().bets
}

val seeWhoHasPlacedABet = Question { actor ->
    actor.use<ParticipateInGames>().playersWhoHavePlacedBets
}

