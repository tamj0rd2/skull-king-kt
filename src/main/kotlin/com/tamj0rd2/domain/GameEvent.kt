package com.tamj0rd2.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class GameEvent {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : GameEvent()

    data class GameStarted(val players: List<PlayerId>) : GameEvent()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : GameEvent()

    data class BidPlaced(val playerId: PlayerId) : GameEvent()

    data class BiddingCompleted(val bids: Map<PlayerId, Int>) : GameEvent()

    data class CardPlayed(val playerId: String, val card: Card) : GameEvent()

    data class TrickStarted(val trickNumber: Int) : GameEvent()

    object TrickCompleted : GameEvent()
    object GameCompleted : GameEvent()
}