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

    fun reject(reason: GameErrorCode): Message = reject(reason.name)
    fun reject(reason: String): Message =
        when (this) {
            is AcceptanceFromClient,
            is AcceptanceFromServer -> error("acceptance cannot be rejected")

            is Rejection -> error("rejection cannot be rejected")
            is KeepAlive -> error("keepAlive cannot be rejected")
            is ToServer,
            is ToClient -> Rejection(id, reason)
        }

    @Serializable
    data class KeepAlive(@Required override val id: MessageId = MessageId.next()) : Message()

    @Serializable
    data class AcceptanceFromServer(override val id: MessageId, val notifications: List<Notification>) : Message() {
        override fun toString(): String {
            return "$id - ${notifications.joinToString(", ")}"
        }
    }

    @Serializable
    data class AcceptanceFromClient(override val id: MessageId) : Message() {
        override fun toString(): String {
            return "$id"
        }
    }

    @Serializable
    data class Rejection(override val id: MessageId, val reason: String) : Message() {
        constructor(id: MessageId, errorCode: GameErrorCode) : this(id, errorCode.name)
    }

    @Serializable
    data class ToClient(val notifications: List<Notification>) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun accept() = AcceptanceFromClient(id)

        override fun toString(): String {
            return "$id - ${notifications.joinToString(", ")}"
        }
    }

    @Serializable
    data class ToServer(val command: PlayerCommand) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun accept(messages: List<Notification>) = AcceptanceFromServer(id, messages)

        override fun toString(): String {
            return "$id - $command"
        }
    }
}
