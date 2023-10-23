package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerId

sealed class Notification {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : Notification()

    data class BidPlaced(val playerId: PlayerId) : Notification()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : Notification()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : Notification()

    data class YourTurn(val cards: List<CardWithPlayability>) : Notification()

    data class TrickCompleted(val winner: PlayerId) : Notification()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : Notification()

    data class YouJoined(val playerId: PlayerId, val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : Notification()

    class GameCompleted : Notification()

    data class GameStarted(val players: List<PlayerId>) : Notification()

    data class RoundStarted(val cardsDealt: List<CardWithPlayability>, val roundNumber: Int) : Notification()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : Notification()
}
