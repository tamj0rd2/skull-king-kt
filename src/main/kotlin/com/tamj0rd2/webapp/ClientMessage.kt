package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName
import java.util.*

sealed class ClientMessage {
    sealed class Notification : ClientMessage() {
        data class UnhandledServerMessage(val offender: String) : Notification()

        data class Error(val stackTrace: String) : Notification()
    }

    sealed class Request : ClientMessage() {
        fun overTheWire() = OverTheWireMessage.ToServer(this, UUID.randomUUID())

        data class PlaceBid(val bid: Int) : Request()

        data class PlayCard(val cardName: CardName) : Request()
    }
}
