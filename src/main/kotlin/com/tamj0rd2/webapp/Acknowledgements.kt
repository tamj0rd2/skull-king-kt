package com.tamj0rd2.webapp

import java.time.Instant
import java.util.*

class Acknowledgements {
    private val outstanding = mutableSetOf<UUID>()
    private val syncObject = Object()

    operator fun invoke(id: UUID) {
        synchronized(syncObject) {
            if (!outstanding.remove(id)) error("you probably tried to re-ack a message - $id")
            syncObject.notify()
        }
    }

    fun waitFor(id: UUID, timeoutMs: Long = 1000) {
        val backoff = timeoutMs / 5

        synchronized(syncObject) {
            outstanding.add(id)

            val mustEndBy = Instant.now().plusMillis(timeoutMs)
            while (Instant.now() < mustEndBy) {
                if (done(id)) return
                syncObject.wait(backoff)
            }

            error("$id not acknowledged. outstanding: $outstanding")
        }
    }

    private fun done(id: UUID) = !outstanding.contains(id)
}