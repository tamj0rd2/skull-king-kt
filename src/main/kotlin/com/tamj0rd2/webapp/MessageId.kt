package com.tamj0rd2.webapp

data class MessageId(val messageId: Int) {
    companion object {
        private var nextId = 0

        val next: MessageId
            get() {
            synchronized(this) {
                nextId += 1
            }
            return MessageId(nextId)
        }
    }
}