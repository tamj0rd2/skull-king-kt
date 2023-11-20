package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerId
import kotlinx.serialization.Serializable

@Serializable
sealed class Notification {

    @Serializable
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : Notification()

    @Serializable
    data class BidPlaced(val playerId: PlayerId) : Notification()

    @Serializable
    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : Notification()

    @Serializable
    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : Notification()

    @Serializable
    data class YourTurn(val cards: List<CardWithPlayability>) : Notification()

    @Serializable
    data class TrickCompleted(val winner: PlayerId) : Notification()

    @Serializable
    data class RoundCompleted(val wins: Map<PlayerId, Int>) : Notification()

    @Serializable
    data class YouJoined(val playerId: PlayerId, val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : Notification()

    @Serializable
    data object GameCompleted : Notification()

    @Serializable
    data class GameStarted(val players: List<PlayerId>) : Notification()

    @Serializable
    data class RoundStarted(val cardsDealt: List<CardWithPlayability>, val roundNumber: Int) : Notification()

    @Serializable
    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : Notification()
}
