package com.tamj0rd2.domain

sealed class GameEvent {
    data class PlayerJoined(val playerId: PlayerId) : GameEvent()

    data class GameStarted(val players: List<PlayerId>) : GameEvent()

    data class RoundStarted(val roundNumber: RoundNumber) : GameEvent()

    data class CardsDealt(val cards: Map<PlayerId, List<Card>>) : GameEvent()

    data class BidPlaced(val playerId: PlayerId, val bid: Bid) : GameEvent()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : GameEvent()

    data class TrickStarted(val trickNumber: TrickNumber, val turnOrder: List<PlayerId>) : GameEvent()

    data class SuitEstablished(val suit: Suit) : GameEvent()

    // TODO: it might be easier to make the argument for this just be a PlayedCard
    data class CardPlayed(val playerId: PlayerId, val card: Card) : GameEvent()

    data class TrickCompleted(val winner: PlayerId) : GameEvent()

    data object GameCompleted : GameEvent()
}
