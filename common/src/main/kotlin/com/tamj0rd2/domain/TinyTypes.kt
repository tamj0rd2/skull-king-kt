package com.tamj0rd2.domain

import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.Value
import dev.forkhandles.values.minValue
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PlayerId(override val value: String): Value<String> {
    companion object {
        val unidentified = PlayerId("unidentified")
    }

    override fun toString(): String = value
}

@JvmInline
@Serializable
value class Bid private constructor(override val value: Int): Value<Int> {
    companion object : IntValueFactory<Bid>(::Bid, 0.minValue)
}
