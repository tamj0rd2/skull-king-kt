package testsupport

import PlayerId

fun interface Question<T> {
    fun ask(actor: Actor): T
}

object WaitingForMorePlayers : Question<Boolean> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().isWaitingForMorePlayers
}

object PlayersAtTheTable : Question<List<PlayerId>> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().playersInRoom
}

object GameHasStarted : Question<Boolean> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().hasGameStarted
}

object TheirCardCount : Question<Int> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().cardCount
}

object TheySeeBets : Question<Map<PlayerId, Int>> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().bets
}

object PlayersWhoHavePlacedABet : Question<List<PlayerId>> {
    override fun ask(actor: Actor) = actor.use<ParticipateInGames>().playersWhoHavePlacedBets
}
