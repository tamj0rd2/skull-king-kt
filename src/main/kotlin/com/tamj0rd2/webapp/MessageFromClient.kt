package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName
import java.util.*

sealed class MessageFromClient {
    data class BidPlaced(val bid: Int) : MessageRequiringAcknowledgement()

    data class CardPlayed(val cardName: CardName) : MessageRequiringAcknowledgement()

    data class UnhandledServerMessage(val offender: String) : MessageFromClient()

    data class Error(val stackTrace: String) : MessageFromClient()

    data class Acknowledgement(val id: UUID) : MessageFromClient()

    sealed class MessageRequiringAcknowledgement() : MessageFromClient() {
        val messageId = UUID.randomUUID()

        fun acknowledge() = MessageToClient.Acknowledgement(messageId)
    }
}