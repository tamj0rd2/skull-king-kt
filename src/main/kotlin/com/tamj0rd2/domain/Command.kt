package com.tamj0rd2.domain

// TODO: can I get to a point where the test contract uses commands instead of Activities directly?
// the test infrastructure can just turn the commands into activities
sealed class Command {

    sealed class GameMasterCommand : Command() {
        object StartGame : GameMasterCommand()

        object StartNextRound : GameMasterCommand()

        object StartNextTrick : GameMasterCommand()

        data class RigDeck(val playerId: PlayerId, val cards: List<Card>) : GameMasterCommand() {
            companion object {
                data class SoThat(val playerId: PlayerId) {
                    fun willEndUpWith(cards: List<Card>) = RigDeck(playerId, cards)
                }
            }
        }

    }

    sealed class PlayerCommand : Command() {
        abstract val actor: PlayerId

        data class JoinGame(override val actor: PlayerId) : PlayerCommand()

        data class PlaceBid(override val actor: PlayerId, val bid: Bid) : PlayerCommand()

        data class PlayCard(override val actor: PlayerId, val cardName: CardName) : PlayerCommand()
    }
}