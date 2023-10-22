package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerId

sealed class MessageToClient {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : MessageToClient()

    data class BidPlaced(val playerId: PlayerId) : MessageToClient()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : MessageToClient()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : MessageToClient()

    data class YourTurn(val cards: List<CardWithPlayability>) : MessageToClient()

    data class TrickCompleted(val winner: PlayerId) : MessageToClient()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : MessageToClient()

    data class YouJoined(val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : MessageToClient() {
        fun overTheWire() = OverTheWireMessage.MessagesToClient(listOf(this))
    }

    class GameCompleted : MessageToClient()

    data class GameStarted(val players: List<PlayerId>) : MessageToClient()

    data class RoundStarted(val cardsDealt: List<CardWithPlayability>, val roundNumber: Int) : MessageToClient()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : MessageToClient()
}
