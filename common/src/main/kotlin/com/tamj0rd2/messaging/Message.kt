package com.tamj0rd2.messaging

import com.tamj0rd2.domain.GameErrorCode
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerState
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
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
    data class AcceptanceFromServer(override val id: MessageId, val state: PlayerState) : Message() {
        override fun toString(): String {
            return "$id"
        }
    }

    @Serializable
    data class AcceptanceFromClient(override val id: MessageId) : Message() {
        override fun toString(): String {
            return "$id"
        }
    }

    @Serializable
    data class Rejection(override val id: MessageId, val reason: String) : Message()

    @Serializable
    data class ToClient(val state: PlayerState) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun accept() = AcceptanceFromClient(id)

        override fun toString(): String {
            return "$id"
        }
    }

    @Serializable
    data class ToServer(val command: PlayerCommand) : Message() {
        @Required
        override val id: MessageId = MessageId.next()

        fun accept(state: PlayerState) = AcceptanceFromServer(id, state)

        override fun toString(): String {
            return "$id - $command"
        }
    }
}
