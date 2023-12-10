package com.tamj0rd2.messaging

import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.GameErrorCode
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import java.util.UUID

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
