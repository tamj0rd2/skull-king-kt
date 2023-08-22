package testsupport

class Question<T>(val fn: (Actor) -> T) {
    fun ask(actor: Actor): T = fn(actor)
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

