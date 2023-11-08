package com.tamj0rd2.webapp

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LockedValue<T> {
    private val lock = Mutex()

    var lockedValue: T? = null
        set(newValue) {
            require(lock.isLocked) { "cannot set the value without first initialising it with use()" }
            field = newValue
        }

    fun use(initialValue: T? = null, block: LockedValue<T>.() -> Unit) {
        require(lockedValue == null) { "the locked value is already erroneously set" }

        runBlocking {
            lock.withLock(this) {
                lockedValue = initialValue
                try {
                    block()
                } finally {
                    lockedValue = null
                }
            }
        }
    }
}