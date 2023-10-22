package com.tamj0rd2.webapp

import org.slf4j.Logger
import java.util.*

sealed class OverTheWireMessage {
    data class MessagesToClient(val messages: List<MessageToClient>) : OverTheWireMessage() {
        val messageId: UUID = UUID.randomUUID()

        fun acknowledge() = AcknowledgementFromClient(messageId)

        override fun toString(): String {
            return "$messageId - ${messages.joinToString(", ")}"
        }
    }

    data class MessageToServer(val message: MessageFromClient, val messageId: UUID = UUID.randomUUID()) :
        OverTheWireMessage() {

        fun acknowledge(messages: List<MessageToClient> = emptyList()) = AcknowledgementFromServer(messageId, messages)

        override fun toString(): String {
            return "$messageId - $message"
        }
    }

    data class AcknowledgementFromServer(val id: UUID, val messages: List<MessageToClient>) : OverTheWireMessage()

    data class AcknowledgementFromClient(val id: UUID) : OverTheWireMessage()
}

fun Logger.receivedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> info("got ack: ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> info("got ack: ${message.id}")
        is OverTheWireMessage.MessageToServer -> info("received: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> info("received: ${message.messageId}")
    }

fun Logger.processedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> info(">> completed << ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> info(">> completed << ${message.id}")
        is OverTheWireMessage.MessageToServer -> info("processed: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> info("processed: ${message.messageId}")
    }

fun Logger.sending(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> info("acking: ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> info("acking: ${message.id}")
        is OverTheWireMessage.MessageToServer -> info(">> sending << $message")
        is OverTheWireMessage.MessagesToClient -> info(">> sending << $message")
    }

fun Logger.awaitingAck(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> error("cannot await an ack of an ack")
        is OverTheWireMessage.AcknowledgementFromServer -> error("cannot await an ack of an ack")
        is OverTheWireMessage.MessageToServer -> info("awaiting ack: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> info("awaiting ack: ${message.messageId}")
    }

fun Logger.sentAckFor(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> error("cannot ack an ack")
        is OverTheWireMessage.AcknowledgementFromServer -> error("cannot ack an ack")
        is OverTheWireMessage.MessageToServer -> info("sent ack: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> info("sent ack: ${message.messageId}")
    }
