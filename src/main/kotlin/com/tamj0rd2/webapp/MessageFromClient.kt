package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject

internal sealed class MessageFromClient {
    val id = MessageId.next

    open val needsAck = true

    data class BidPlaced(val bid: Int) : MessageFromClient()

    data class CardPlayed(val cardName: CardName) : MessageFromClient()

    data class UnhandledServerMessage(val offender: String) : MessageFromClient()

    data class Error(val stackTrace: String) : MessageFromClient()

    class JoinGame : MessageFromClient() {
        override fun toString(): String {
            return "JoinGame()"
        }
    }

    data class Ack(val acked: MessageToClient) : MessageFromClient() {
        override val needsAck = false

        init {
            require(acked !is MessageToClient.Ack)
        }
    }

    fun ack() = MessageToClient.Ack(this)

    fun json() = this.asJsonObject().asCompactJsonString()
}