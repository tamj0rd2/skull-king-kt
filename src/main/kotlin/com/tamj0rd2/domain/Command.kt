package com.tamj0rd2.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class GameMasterCommand {
    override fun toString(): String {
        return this::class.simpleName!!
    }

    @Serializable
    object StartGame : GameMasterCommand()

    @Serializable
    object StartNextRound : GameMasterCommand()

    @Serializable
    object StartNextTrick : GameMasterCommand()

    @Serializable
    data class RigDeck(val playerId: PlayerId, val cards: List<Card>) : GameMasterCommand() {
        companion object {
            data class SoThat(val playerId: PlayerId) {
                fun willEndUpWith(cards: List<Card>) = RigDeck(playerId, cards)
            }
        }
    }

}

@Serializable
sealed class PlayerCommand {
    abstract val actor: PlayerId

    @Serializable
    data class JoinGame(override val actor: PlayerId) : PlayerCommand()

    @Serializable
    data class PlaceBid(override val actor: PlayerId, val bid: Bid) : PlayerCommand()

    @Serializable
    data class PlayCard(override val actor: PlayerId, val cardName: CardName) : PlayerCommand()
}
