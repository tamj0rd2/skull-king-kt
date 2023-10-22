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

    data class AcknowledgementFromServer(val id: UUID, val messages: List<MessageToClient>) : OverTheWireMessage() {
        override fun toString(): String {
            return "$id - ${messages.joinToString(", ")}"
        }
    }

    data class AcknowledgementFromClient(val id: UUID) : OverTheWireMessage() {
        override fun toString(): String {
            return "$id"
        }
    }
}

fun Logger.receivedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("got ack: ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> debug("got ack: ${message.id}")
        is OverTheWireMessage.MessageToServer -> debug("received: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("received: ${message.messageId}")
    }

fun Logger.processedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug(">> completed << ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> debug(">> completed << ${message.id}")
        is OverTheWireMessage.MessageToServer -> debug("processed: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("processed: ${message.messageId}")
    }

fun Logger.sending(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("acking: $message")
        is OverTheWireMessage.AcknowledgementFromServer -> if (message.messages.isNotEmpty()) info("acking: $message") else debug("acking: $message")
        is OverTheWireMessage.MessageToServer -> info("sending: $message")
        is OverTheWireMessage.MessagesToClient -> info("sending: $message")
    }

fun Logger.awaitingAck(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> error("cannot await an ack of an ack")
        is OverTheWireMessage.AcknowledgementFromServer -> error("cannot await an ack of an ack")
        is OverTheWireMessage.MessageToServer -> debug("awaiting ack: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("awaiting ack: ${message.messageId}")
    }

fun Logger.sentAck(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("sent ack: $message")
        is OverTheWireMessage.AcknowledgementFromServer -> debug("sent ack: $message")
        is OverTheWireMessage.MessageToServer -> error("not an ack")
        is OverTheWireMessage.MessagesToClient -> error("not an ack")
    }
