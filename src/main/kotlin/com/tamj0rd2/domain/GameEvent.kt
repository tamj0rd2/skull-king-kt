package com.tamj0rd2.domain

sealed class GameEvent {
    abstract val type: Type

    enum class Type {
        PlayerJoined,
        GameStarted,
        RoundStarted,
        BetPlaced,
        BettingCompleted,
        CardPlayed,
    }

    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : GameEvent() {
        override val type: Type = Type.PlayerJoined
    }

    class GameStarted : GameEvent() {
        override val type: Type = Type.GameStarted
    }

    data class RoundStarted(val cardsDealt: List<Card>) : GameEvent() {
        override val type: Type = Type.RoundStarted
    }

    data class BetPlaced(val playerId: PlayerId, val isBettingComplete: Boolean) : GameEvent() {
        override val type: Type = Type.BetPlaced
    }

    data class BettingCompleted(val bets: Map<PlayerId, Int>) : GameEvent() {
        override val type: Type = Type.BettingCompleted
    }

    data class CardPlayed(val playerId: String, val cardId: CardId) : GameEvent() {
        override val type: Type = Type.CardPlayed
    }
}