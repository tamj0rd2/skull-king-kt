package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerId

sealed class ServerMessage {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : ServerMessage()

    data class BidPlaced(val playerId: PlayerId) : ServerMessage()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : ServerMessage()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : ServerMessage()

    data class YourTurn(val cards: List<CardWithPlayability>) : ServerMessage()

    data class TrickCompleted(val winner: PlayerId) : ServerMessage()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : ServerMessage()

    data class YouJoined(val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : ServerMessage() {
        fun overTheWire() = OverTheWireMessage.ToClient(listOf(this))
    }

    class GameCompleted : ServerMessage()

    data class GameStarted(val players: List<PlayerId>) : ServerMessage()

    data class RoundStarted(val cardsDealt: List<CardWithPlayability>, val roundNumber: Int) : ServerMessage()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : ServerMessage()
}
