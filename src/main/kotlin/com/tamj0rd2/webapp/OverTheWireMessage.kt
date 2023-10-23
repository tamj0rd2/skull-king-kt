package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Command.PlayerCommand
import org.slf4j.Logger
import java.util.*

data class MessageId(val value: UUID) {
    companion object {
        fun next() = MessageId(UUID.randomUUID())
    }
}

sealed class OverTheWireMessage {
    abstract val id: MessageId

    fun nack(): OverTheWireMessage =
        when (this) {
            is Ack -> error("ack cannot be nacked")
            is Nack -> error("nack cannot be nacked")
            is ToServer -> Nack(id)
            is ToClient -> Nack(id)
        }

    sealed class Ack : OverTheWireMessage() {
        data class FromServer(override val id: MessageId, val messages: List<ServerMessage>) : Ack() {
            override fun toString(): String {
                return "$id - ${messages.joinToString(", ")}"
            }
        }

        data class FromClient(override val id: MessageId) : Ack() {
            override fun toString(): String {
                return "$id"
            }
        }
    }

    data class Nack(override val id: MessageId) : OverTheWireMessage()

    data class ToClient(val messages: List<ServerMessage>) : OverTheWireMessage() {
        override val id: MessageId = MessageId.next()

        fun acknowledge() = Ack.FromClient(id)

        override fun toString(): String {
            return "$id - ${messages.joinToString(", ")}"
        }
    }

    data class ToServer(val command: PlayerCommand) : OverTheWireMessage() {
        override val id: MessageId = MessageId.next()

        fun acknowledge(messages: List<ServerMessage> = emptyList()) = Ack.FromServer(id, messages)

        override fun toString(): String {
            return "$id - $command"
        }
    }
}

fun Logger.receivedMessage(message: OverTheWireMessage) =
    when (message) {
        is OverTheWireMessage.Ack -> debug("got ack: {}", message.id)
        is OverTheWireMessage.ToServer -> debug("received: {}", message.id)
        is OverTheWireMessage.ToClient -> debug("received: {}", message.id)
        is OverTheWireMessage.Nack -> debug("received: {}", message)
    }

fun Logger.processedMessage(message: OverTheWireMessage) =
    when (message) {
        is OverTheWireMessage.Ack -> debug(">> completed << {}", message.id)
        is OverTheWireMessage.ToServer -> debug("processed: {}", message.id)
        is OverTheWireMessage.ToClient -> debug("processed: {}", message.id)
        is OverTheWireMessage.Nack -> TODO()
    }

fun Logger.sending(message: OverTheWireMessage) =
    when (message) {
        is OverTheWireMessage.Ack.FromClient -> debug("acking: {}", message)
        is OverTheWireMessage.Ack.FromServer -> {
            if (message.messages.isNotEmpty()) info("acking: $message") else debug("acking: {}", message)
        }

        is OverTheWireMessage.ToServer -> info("sending: $message")
        is OverTheWireMessage.ToClient -> info("sending: $message")
        is OverTheWireMessage.Nack -> info("sending: $message")
    }

fun Logger.awaitingAck(message: OverTheWireMessage) =
    when (message) {
        is OverTheWireMessage.Ack -> error("cannot await an ack of an ack")
        is OverTheWireMessage.ToServer -> debug("awaiting ack: {}", message.id)
        is OverTheWireMessage.ToClient -> debug("awaiting ack: {}", message.id)
        is OverTheWireMessage.Nack -> error("cannot await an ack of a proessing failure")
    }

fun Logger.sentMessage(message: OverTheWireMessage) =
    when (message) {
        is OverTheWireMessage.Ack -> debug("sent ack: {}", message)
        is OverTheWireMessage.ToServer -> debug("sent message: {}", message.id)
        is OverTheWireMessage.ToClient -> debug("sent message: {}", message.id)
        is OverTheWireMessage.Nack -> debug("sent processing failure: {}", message)
    }
