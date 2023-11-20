package com.tamj0rd2.webapp

import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.GameErrorCode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.slf4j.Logger
import java.util.*

@JvmInline
@Serializable
value class MessageId(val value: String) {
    override fun toString(): String {
        return value
    }

    companion object {
        fun next() = MessageId(UUID.randomUUID().toString())
    }
}

@Serializable
sealed class Message {
    abstract val id: MessageId

    fun nack(reason: GameErrorCode): Message = nack(reason.name)
    fun nack(reason: String): Message =
        when (this) {
            is AckFromClient,
            is AckFromServer -> error("ack cannot be nacked")

            is Nack -> error("nack cannot be nacked")
            is KeepAlive -> error("keepAlive cannot be nacked")
            is ToServer,
            is ToClient -> Nack(id, reason)
        }

    @Serializable
    data class KeepAlive(@Required override val id: MessageId = MessageId.next()) : Message()

    @Serializable
    data class AckFromServer(override val id: MessageId, val notifications: List<Notification>) : Message() {
        override fun toString(): String {
            return "$id - ${notifications.joinToString(", ")}"
        }
    }

    @Serializable
    data class AckFromClient(override val id: MessageId) : Message() {
        override fun toString(): String {
            return "$id"
        }
    }

    @Serializable
    data class Nack(override val id: MessageId, val reason: String) : Message() {
        constructor(id: MessageId, errorCode: GameErrorCode) : this(id, errorCode.name)
    }

    @Serializable
    data class ToClient(val notifications: List<Notification>) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun acknowledge() = AckFromClient(id)

        override fun toString(): String {
            return "$id - ${notifications.joinToString(", ")}"
        }
    }

    @Serializable
    data class ToServer(val command: PlayerCommand) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun acknowledge(messages: List<Notification>) = AckFromServer(id, messages)

        override fun toString(): String {
            return "$id - $command"
        }
    }
}

fun Logger.receivedMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug("got ack: {}", message.id)

        is Message.ToServer -> info("received: {}", message)
        is Message.ToClient -> debug("received: {}", message.id)
        is Message.Nack -> debug("received: {}", message)
        is Message.KeepAlive -> {}
    }

fun Logger.processedMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug(">> completed << {}", message.id)

        is Message.ToServer -> debug("processed: {}", message.id)
        is Message.ToClient -> debug("processed: {}", message.id)
        is Message.Nack -> TODO()
        is Message.KeepAlive -> {}
    }

fun Logger.sending(message: Message) =
    when (message) {
        is Message.AckFromClient -> debug("acking: {}", message)
        is Message.AckFromServer -> {
            if (message.notifications.isNotEmpty()) info("acking: $message") else debug("acking: {}", message)
        }

        is Message.ToServer -> info("sending: $message")
        is Message.ToClient -> info("sending: $message")
        is Message.Nack -> info("sending: $message")
        is Message.KeepAlive -> {}
    }

fun Logger.awaitingAck(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> error("cannot await an ack of an ack")

        is Message.ToServer -> debug("awaiting ack: {}", message.id)
        is Message.ToClient -> debug("awaiting ack: {}", message.id)
        is Message.Nack -> error("cannot await an ack of a proessing failure")
        is Message.KeepAlive -> {}
    }

fun Logger.sentMessage(message: Message) =
    when (message) {
        is Message.AckFromClient,
        is Message.AckFromServer -> debug("sent ack: {}", message)

        is Message.ToServer -> debug("sent message: {}", message.id)
        is Message.ToClient -> debug("sent message: {}", message.id)
        is Message.Nack -> debug("sent processing failure: {}", message)
        is Message.KeepAlive -> {}
    }
