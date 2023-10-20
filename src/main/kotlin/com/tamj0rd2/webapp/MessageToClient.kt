package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.PlayerId
import java.util.*

internal sealed class MessageToClient {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : MessageToClient()

    data class BidPlaced(val playerId: PlayerId) : MessageToClient()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : MessageRequiringAcknowledgement()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : MessageToClient()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : MessageToClient()

    data class YourTurn(val cards: Map<CardName, Boolean>) : MessageToClient()

    data class TrickCompleted(val winner: PlayerId) : MessageToClient()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : MessageToClient()

    data class YouJoined(val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : MessageToClient()

    object GameCompleted : MessageToClient()

    data class Multi(val messages: List<MessageToClient>) : MessageToClient()

    data class GameStarted(val players: List<PlayerId>) : MessageRequiringAcknowledgement()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : MessageRequiringAcknowledgement()

    sealed class MessageRequiringAcknowledgement: MessageToClient() {
        fun acknowledge() = MessageFromClient.Acknowledgement(messageId)

        val messageId = UUID.randomUUID()
    }
}
