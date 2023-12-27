package com.tamj0rd2.webapp

import com.tamj0rd2.messaging.MessageId
import com.tamj0rd2.webapp.AnswerException.*
import java.time.Instant

typealias NackReason = String

class AnswerTracker(private val timeoutMs: Long) {
    private val outstanding = mutableSetOf<MessageId>()
    private val nacks = mutableMapOf<MessageId, String>()
    private val syncObject = Object()

    fun markAsAccepted(id: MessageId) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAcceptedException(id)
            syncObject.notify()
        }
    }

    fun markAsRejected(id: MessageId, reason: String) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) throw ReAcceptedException(id)
            if (nacks.contains(id)) throw ReRejectedException(id)
            nacks[id] = reason
            syncObject.notify()
        }
    }

    fun waitForAnswer(id: MessageId, before: () -> Unit): NackReason? {
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

            throw NotAnsweredException(id, this)
        }
    }

    override fun toString(): String {
        return "outstanding=$outstanding, nacks=$nacks"
    }
}

sealed class AnswerException(message: String) : Exception(message) {
    data class ReAcceptedException(val messageId: MessageId) :
        AnswerException("you probably tricked to re-accept a message - $messageId")

    data class ReRejectedException(val messageId: MessageId) :
        AnswerException("you probably tricked to re-reject a message - $messageId")

    data class NotAnsweredException(val messageId: MessageId, val answerTracker: AnswerTracker) :
        AnswerException("the message was not answered within the given timeout - $answerTracker")
}
