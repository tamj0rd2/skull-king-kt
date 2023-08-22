package testsupport

open class Question<T>(private val description: String, private val answer: (Actor) -> T) {
    fun ask(actor: Actor): T = answer(actor)
    override fun toString(): String  = "question about $description"
    companion object {
        fun <R> about(description: String, answer: (Actor) -> R) = object : Question<R>(description, answer) {}
    }
}

val ThePlayersAtTheTable = Question.about("the players at the table") { actor ->
    actor.use<ParticipateInGames>().playersInRoom
}

val TheGameState = Question.about("the game state") { actor ->
    actor.use<ParticipateInGames>().gameState
}

val TheirHand = Question.about("their hand") { actor ->
    actor.use<ParticipateInGames>().hand
}

val TheySeeBets = Question.about("the bets that have been made") { actor ->
    actor.use<ParticipateInGames>().bets
}

val ThePlayersWhoHavePlacedABet = Question.about("asked which players have placed a bet") { actor ->
    actor.use<ParticipateInGames>().playersWhoHavePlacedBets
}
