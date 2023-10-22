package com.tamj0rd2.webapp

import com.tamj0rd2.webapp.AcknowledgementException.*
import java.time.Instant
import java.util.*

class Acknowledgements(private val timeoutMs: Long) {
    private val outstanding = mutableSetOf<UUID>()
    private val nacks = mutableSetOf<UUID>()
    private val syncObject = Object()

    fun ack(id: UUID) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAckException(id)
            syncObject.notify()
        }
    }

    fun nack(id: UUID) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAckException(id)
            if (!nacks.add(id)) throw ReNackException(id)
            syncObject.notify()
        }
    }

    fun waitFor(id: UUID, before: () -> Unit): Result<Unit> {
        val backoff = timeoutMs / 5

        return synchronized(syncObject) {
            outstanding.add(id)

            before()

            val mustEndBy = Instant.now().plusMillis(timeoutMs)
            while (Instant.now() < mustEndBy) {
                if (nacks.contains(id)) return@synchronized Result.failure<Unit>(NackException(id))
                if (!outstanding.contains(id)) return@synchronized Result.success(Unit)
                syncObject.wait(backoff)
            }

            throw NotAcknowledgedException(id, this)
        }
    }

    override fun toString(): String {
        return "outstanding=$outstanding, nacks=$nacks"
    }
}

sealed class AcknowledgementException(message: String) : Exception(message) {
    data class ReAckException(val messageId: UUID) :
        AcknowledgementException("you probably tricked to re-ack a message - $messageId")

    data class ReNackException(val messageId: UUID) :
        AcknowledgementException("you probably tricked to re-nack a message - $messageId")

    data class NackException(val messageId: UUID) :
        AcknowledgementException("the message was nacked, indicating a failure - $messageId")

    data class NotAcknowledgedException(val messageId: UUID, val acknowledgements: Acknowledgements) :
        AcknowledgementException("the message was not acknowledged within the given timeout - $acknowledgements")
}
