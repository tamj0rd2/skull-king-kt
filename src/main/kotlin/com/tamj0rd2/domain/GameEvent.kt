package com.tamj0rd2.domain

sealed class GameEvent {
    val timestamp = System.currentTimeMillis()

    data class GameStarted(val players: List<PlayerId>) : GameEvent()

    data class PlayerJoined(val playerId: PlayerId) : GameEvent()

    data class RoundStarted(val roundNumber: Int) : GameEvent()

    data class CardsDealt(val cards: Map<PlayerId, List<Card>>) : GameEvent()

    data class BidPlaced(val playerId: PlayerId, val bid: Bid) : GameEvent()

    // TODO: use Bid here, but also, BiddingCompleted isn't a thing that really happens. People just bid until they're done.
    data class BiddingCompleted(val bids: Map<PlayerId, Int>) : GameEvent()

    data class TrickStarted(val trickNumber: Int) : GameEvent()

    data class CardPlayed(val playerId: PlayerId, val card: Card) : GameEvent()

    data class TrickCompleted(val winner: PlayerId) : GameEvent()

    object GameCompleted : GameEvent()
}
