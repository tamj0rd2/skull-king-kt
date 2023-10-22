package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName

sealed class MessageFromClient {
    fun overTheWire() = OverTheWireMessage.MessageToServer(this)

    data class BidPlaced(val bid: Int) : MessageFromClient()

    data class CardPlayed(val cardName: CardName) : MessageFromClient()

    data class UnhandledServerMessage(val offender: String) : MessageFromClient()

    data class Error(val stackTrace: String) : MessageFromClient()
}