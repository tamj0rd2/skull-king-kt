package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Command.PlayerCommand
import com.tamj0rd2.domain.GameErrorCode
import org.slf4j.Logger
import java.util.*

data class MessageId(val value: UUID) {
    override fun toString(): String {
        return value.toString()
    }

    companion object {
        fun next() = MessageId(UUID.randomUUID())
    }
}

sealed class Message {
    abstract val id: MessageId

    fun nack(reason: GameErrorCode): Message = nack(reason.name)
    fun nack(reason: String): Message =
        when (this) {
            is Ack -> error("ack cannot be nacked")
            is Nack -> error("nack cannot be nacked")
            is KeepAlive -> error("keepAlive cannot be nacked")
            is ToServer,
            is ToClient -> Nack(id, reason)
        }

    // TODO: Look into using the RFC instead. https://www.rfc-editor.org/rfc/rfc6455#section-5.5.2
    data class KeepAlive(override val id: MessageId = MessageId.next()) : Message()

    sealed class Ack : Message() {
        data class FromServer(override val id: MessageId, val notifications: List<Notification>) : Ack() {
            override fun toString(): String {
                return "$id - ${notifications.joinToString(", ")}"
            }
        }

        data class FromClient(override val id: MessageId) : Ack() {
            override fun toString(): String {
                return "$id"
            }
        }
    }

    data class Nack(override val id: MessageId, val reason: String) : Message() {
        constructor(id: MessageId, errorCode: GameErrorCode) : this(id, errorCode.name)
    }

    data class ToClient(val notifications: List<Notification>) : Message() {
        override val id: MessageId = MessageId.next()

        fun acknowledge() = Ack.FromClient(id)

        override fun toString(): String {
            return "$id - ${notifications.joinToString(", ")}"
        }
    }

    data class ToServer(val command: PlayerCommand) : Message() {
        override val id: MessageId = MessageId.next()

        fun acknowledge(messages: List<Notification>) = Ack.FromServer(id, messages)

        override fun toString(): String {
            return "$id - $command"
        }
    }
}

fun Logger.receivedMessage(message: Message) =
    when (message) {
        is Message.Ack -> debug("got ack: {}", message.id)
        is Message.ToServer -> debug("received: {}", message)
        is Message.ToClient -> debug("received: {}", message.id)
        is Message.Nack -> debug("received: {}", message)
        is Message.KeepAlive -> {}
    }

fun Logger.processedMessage(message: Message) =
    when (message) {
        is Message.Ack -> debug(">> completed << {}", message.id)
        is Message.ToServer -> debug("processed: {}", message.id)
        is Message.ToClient -> debug("processed: {}", message.id)
        is Message.Nack -> TODO()
        is Message.KeepAlive -> {}
    }

fun Logger.sending(message: Message) =
    when (message) {
        is Message.Ack.FromClient -> debug("acking: {}", message)
        is Message.Ack.FromServer -> {
            if (message.notifications.isNotEmpty()) info("acking: $message") else debug("acking: {}", message)
        }

        is Message.ToServer -> info("sending: $message")
        is Message.ToClient -> info("sending: $message")
        is Message.Nack -> info("sending: $message")
        is Message.KeepAlive -> {}
    }

fun Logger.awaitingAck(message: Message) =
    when (message) {
        is Message.Ack -> error("cannot await an ack of an ack")
        is Message.ToServer -> debug("awaiting ack: {}", message.id)
        is Message.ToClient -> debug("awaiting ack: {}", message.id)
        is Message.Nack -> error("cannot await an ack of a proessing failure")
        is Message.KeepAlive -> {}
    }

fun Logger.sentMessage(message: Message) =
    when (message) {
        is Message.Ack -> debug("sent ack: {}", message)
        is Message.ToServer -> debug("sent message: {}", message.id)
        is Message.ToClient -> debug("sent message: {}", message.id)
        is Message.Nack -> debug("sent processing failure: {}", message)
        is Message.KeepAlive -> {}
    }
