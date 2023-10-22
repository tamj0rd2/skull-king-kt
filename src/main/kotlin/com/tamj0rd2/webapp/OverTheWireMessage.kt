package com.tamj0rd2.webapp

import org.slf4j.Logger
import java.util.*

sealed class OverTheWireMessage {

    fun processingFailed(): OverTheWireMessage =
        when(this) {
            is AcknowledgementFromClient -> error("processing an ack should never fail")
            is AcknowledgementFromServer -> error("processing an ack should never fail")
            is MessageToServer -> ProcessingFailure(messageId)
            is MessagesToClient -> ProcessingFailure(messageId)
            is ProcessingFailure -> error("you're already working with a processing failure")
        }

    // TODO: this naming is really confusing now
    data class MessagesToClient(val messages: List<MessageToClient>) : OverTheWireMessage() {
        val messageId: UUID = UUID.randomUUID()

        fun acknowledge() = AcknowledgementFromClient(messageId)

        override fun toString(): String {
            return "$messageId - ${messages.joinToString(", ")}"
        }
    }

    // TODO: this naming is really confusing now
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

    data class ProcessingFailure(val id: UUID): OverTheWireMessage()
}

fun Logger.receivedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("got ack: ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> debug("got ack: ${message.id}")
        is OverTheWireMessage.MessageToServer -> debug("received: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("received: ${message.messageId}")
        is OverTheWireMessage.ProcessingFailure -> debug("received: $message")
    }

fun Logger.processedMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug(">> completed << ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> debug(">> completed << ${message.id}")
        is OverTheWireMessage.MessageToServer -> debug("processed: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("processed: ${message.messageId}")
        is OverTheWireMessage.ProcessingFailure -> TODO()
    }

fun Logger.sending(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("acking: $message")
        is OverTheWireMessage.AcknowledgementFromServer -> if (message.messages.isNotEmpty()) info("acking: $message") else debug("acking: $message")
        is OverTheWireMessage.MessageToServer -> info("sending: $message")
        is OverTheWireMessage.MessagesToClient -> info("sending: $message")
        is OverTheWireMessage.ProcessingFailure -> info("sending: $message")
    }

fun Logger.awaitingAck(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> error("cannot await an ack of an ack")
        is OverTheWireMessage.AcknowledgementFromServer -> error("cannot await an ack of an ack")
        is OverTheWireMessage.MessageToServer -> debug("awaiting ack: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("awaiting ack: ${message.messageId}")
        is OverTheWireMessage.ProcessingFailure -> error("cannot await an ack of a proessing failure")
    }

fun Logger.sentMessage(message: OverTheWireMessage) =
    when(message) {
        is OverTheWireMessage.AcknowledgementFromClient -> debug("sent ack: ${message.id}")
        is OverTheWireMessage.AcknowledgementFromServer -> debug("sent ack: $message")
        is OverTheWireMessage.MessageToServer -> debug("sent message: ${message.messageId}")
        is OverTheWireMessage.MessagesToClient -> debug("sent message: ${message.messageId}")
        is OverTheWireMessage.ProcessingFailure -> info("sent processing failure: $message")
    }
