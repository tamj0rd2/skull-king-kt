package testsupport

open class Question<T>(private val description: String, private val answer: (Actor) -> T) {
    fun ask(actor: Actor): T = answer(actor)
    override fun toString(): String  = "question about $description"
    companion object {
        fun <T> about(description: String, answer: (Actor) -> T) = object : Question<T>(description, answer) {}
    }
}

val TheyAreWaitingForMorePlayers = Question.about("whether they are waiting for more players") { actor ->
    actor.use<ParticipateInGames>().isWaitingForMorePlayers
}

val ThePlayersAtTheTable = Question.about("the players at the table") { actor ->
    actor.use<ParticipateInGames>().playersInRoom
}

// TODO: this should probably be "a question about the game state" / TheGameState
// TODO: then I can get rid of WaitingForMorePlayers
val TheGameHasStarted = Question.about("whether the game has started") { actor ->
    actor.use<ParticipateInGames>().hasGameStarted
}

// TODO: this should probably be "a question about their hand"
val TheirCardCount = Question.about("their card count") { actor ->
    actor.use<ParticipateInGames>().cardCount
}

val TheySeeBets = Question.about("the bets that have been made") { actor ->
    actor.use<ParticipateInGames>().bets
}

val ThePlayersWhoHavePlacedABet = Question.about("asked which players have placed a bet") { actor ->
    actor.use<ParticipateInGames>().playersWhoHavePlacedBets
}
