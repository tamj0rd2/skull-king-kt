package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.PlayerId
import java.util.*

sealed class MessageToClient {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : MessageToClient()

    data class BidPlaced(val playerId: PlayerId) : MessageToClient()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : MessageToClient()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : MessageToClient()

    data class YourTurn(val cards: List<CardWithPlayability>) : MessageToClient()

    data class TrickCompleted(val winner: PlayerId) : MessageToClient()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : MessageToClient()

    data class YouJoined(val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : MessageToClient()

    object GameCompleted : MessageToClient()

    data class Multi(val messages: List<MessageToClient>) : MessageToClient() {
        fun messagesRequiringAcknowledgement() = messages.filterIsInstance<MessageRequiringAcknowledgement>()
    }

    data class Acknowledgement(val messageId: UUID) : MessageToClient()

    data class GameStarted(val players: List<PlayerId>) : MessageRequiringAcknowledgement()

    data class RoundStarted(val cardsDealt: List<CardWithPlayability>, val roundNumber: Int) : MessageRequiringAcknowledgement()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : MessageRequiringAcknowledgement()

    sealed class MessageRequiringAcknowledgement : MessageToClient() {
        fun acknowledge() = MessageFromClient.Acknowledgement(messageId)

        val messageId = UUID.randomUUID()
    }
}
