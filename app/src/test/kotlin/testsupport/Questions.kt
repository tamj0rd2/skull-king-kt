package testsupport

open class Question<T>(private val description: String, private val answer: (Actor) -> T) {
    fun answeredBy(actor: Actor): T = answer(actor)
    override fun toString(): String  = "question about $description"
    companion object {
        fun <R> about(description: String, answer: (Actor) -> R) = object : Question<R>(description, answer) {}
    }
}

val ThePlayersAtTheTable = Question.about("the players at the table") { actor ->
    actor.gameState.playersInRoom
}

// NOTE: these could all be rolled up into a single question about the game state...
val TheRoundNumber = Question.about("the round number") { actor ->
    actor.gameState.roundNumber
}

val TheTrickNumber = Question.about("the trick number") { actor ->
    actor.gameState.trickNumber
}

val TheGameState = Question.about("the game state") { actor ->
    actor.gameState.gameState
}

val TheRoundPhase = Question.about("the round phase") { actor ->
    actor.gameState.roundPhase
}

val TheCurrentPlayer = Question.about("the current player") { actor ->
    actor.gameState.currentPlayer
}

val TheWinnerOfTheTrick = Question.about("the winner of the trick") { actor ->
    actor.gameState.trickWinner
}

val TheirHand = Question.about("their hand") { actor ->
    actor.gameState.hand
}
val HisHand = TheirHand
val HerHand = TheirHand

val TheirFirstCard = Question.about("the first card in their hand") { actor ->
    val hand = actor.gameState.hand
    hand.first().card
}

val TheySeeBids = Question.about("the bids that have been placed") { actor ->
    actor.gameState.bids
}

val TheySeeWinsOfTheRound = Question.about("the actual wins during the round") { actor ->
    actor.gameState.winsOfTheRound
}

val TheCurrentTrick = Question.about("the current trick") { actor ->
    actor.gameState.trick
}
