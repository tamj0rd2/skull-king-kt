package com.tamj0rd2.domain

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PlayerId(val value: String) {
    // toString is needed because the default representation of a value class is the same as with data classes. i.e PlayerId(playerId: String)
    override fun toString(): String {
        return value
    }

    companion object {
        val unidentified = PlayerId("unidentified")
    }
}
