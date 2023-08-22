package testsupport

import PlayerId

sealed class Question<T>(private val fn: (Actor) -> T) {
    fun ask(actor: Actor): T = fn(actor)
}

class waitingForMorePlayers : Question<Boolean>({ actor -> actor.use<ParticipateInGames>().isWaitingForMorePlayers })

class playersAtTheTable : Question<List<PlayerId>>({ actor -> actor.use<ParticipateInGames>().playersInRoom })

class gameHasStarted : Question<Boolean>({ actor -> actor.use<ParticipateInGames>().hasGameStarted })

class theirCardCount : Question<Int>({ actor -> actor.use<ParticipateInGames>().cardCount })

class theySeeBets : Question<Map<PlayerId, Int>>({ actor -> actor.use<ParticipateInGames>().bets })

class seeWhoHasPlacedABet : Question<List<PlayerId>>({ actor -> actor.use<ParticipateInGames>().playersWhoHavePlacedBets })

