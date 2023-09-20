package com.tamj0rd2.domain

// TODO: GameEvents are a bit of a lie. BidPlaced with only a playerId is not a real thing that happens. A player makes
// a specific bid. The messaging to the frontend is being tangled up with the domain model here. It's a bad smell that
// Game.kt distributes messages despite the InMemory version not needing to make use of it at all.
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
