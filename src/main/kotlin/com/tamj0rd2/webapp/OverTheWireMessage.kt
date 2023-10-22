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
        is OverTheWireMessage.AcknowledgementFromClient -> info("acking: $message")
        is OverTheWireMessage.AcknowledgementFromServer -> info("acking: $message")
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

fun Logger.sentAck(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> info("sent ack: $message")
        is OverTheWireMessage.AcknowledgementFromServer -> info("sent ack: $message")
        is OverTheWireMessage.MessageToServer -> error("not an ack")
        is OverTheWireMessage.MessagesToClient -> error("not an ack")
    }
