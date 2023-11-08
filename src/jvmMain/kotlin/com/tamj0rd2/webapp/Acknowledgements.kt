package com.tamj0rd2.webapp

import com.tamj0rd2.webapp.AcknowledgementException.*
import java.time.Instant

typealias NackReason = String

class Acknowledgements(private val timeoutMs: Long) {
    private val outstanding = mutableSetOf<MessageId>()
    private val nacks = mutableMapOf<MessageId, String>()
    private val syncObject = Object()

    fun ack(id: MessageId) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAckException(id)
            syncObject.notify()
        }
    }

    fun nack(id: MessageId, reason: String) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAckException(id)
            if (nacks.contains(id)) throw ReNackException(id)
            nacks[id] = reason
            syncObject.notify()
        }
    }

    fun waitFor(id: MessageId, before: () -> Unit): NackReason? {
        val backoff = timeoutMs / 5

        return synchronized(syncObject) {
            outstanding.add(id)

            before()

            val mustEndBy = Instant.now().plusMillis(timeoutMs)
            while (Instant.now() < mustEndBy) {
                nacks[id]?.let { return@synchronized it }
                if (!outstanding.contains(id)) return@synchronized null
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
    data class ReAckException(val messageId: MessageId) :
        AcknowledgementException("you probably tricked to re-ack a message - $messageId")

    data class ReNackException(val messageId: MessageId) :
        AcknowledgementException("you probably tricked to re-nack a message - $messageId")

    data class NackException(val messageId: MessageId) :
        AcknowledgementException("the message was nacked, indicating a failure - $messageId")

    data class NotAcknowledgedException(val messageId: MessageId, val acknowledgements: Acknowledgements) :
        AcknowledgementException("the message was not acknowledged within the given timeout - $acknowledgements")
}
