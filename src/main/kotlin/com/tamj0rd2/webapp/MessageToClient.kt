package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject

internal sealed class MessageToClient {
    val id = MessageId.next

    open val needsAck = true

    data class PlayerJoined(
        val playerId: PlayerId,
        val players: List<PlayerId>,
        val waitingForMorePlayers: Boolean,
        override val needsAck: Boolean,
    ) : MessageToClient()

    data class GameStarted(val players: List<PlayerId>) : MessageToClient()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : MessageToClient()

    data class BidPlaced(val playerId: PlayerId) : MessageToClient()

    data class BiddingCompleted(val bids: Map<PlayerId, Bid>) : MessageToClient()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : MessageToClient()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : MessageToClient()

    data class YourTurn(val cards: Map<CardName, Boolean>) : MessageToClient()

    data class TrickCompleted(val winner: PlayerId) : MessageToClient()

    data class RoundCompleted(val wins: Map<PlayerId, Int>) : MessageToClient()

    class GameCompleted : MessageToClient()

    data class Multi(val messages: List<MessageToClient>) : MessageToClient()

    data class Ack(val acked: MessageFromClient) : MessageToClient() {
        override val needsAck = false

        init {
            require(acked !is MessageFromClient.Ack)
        }
    }

    fun ack() = MessageFromClient.Ack(this)

    companion object {
        fun multi(vararg messages: MessageToClient) = multi(messages.toList())
        fun multi(messages: List<MessageToClient>) = if (messages.size > 1) Multi(messages.toList()) else messages.single()
    }

    fun json() = this.asJsonObject().asCompactJsonString()
}

