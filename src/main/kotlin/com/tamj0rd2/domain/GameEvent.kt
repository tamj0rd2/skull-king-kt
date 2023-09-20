package com.tamj0rd2.domain

sealed class GameEvent {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : GameEvent()

    data class GameStarted(val players: List<PlayerId>) : GameEvent()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : GameEvent()

    data class BidPlaced(val playerId: PlayerId) : GameEvent()

    data class BiddingCompleted(val bids: Map<PlayerId, Int>) : GameEvent()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : GameEvent()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : GameEvent()

    object TrickCompleted : GameEvent()
    object GameCompleted : GameEvent()
}
