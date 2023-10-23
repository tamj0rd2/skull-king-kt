package com.tamj0rd2.domain

sealed class Command {

    sealed class GameMasterCommand : Command() {
        override fun toString(): String {
            return this::class.simpleName!!
        }

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