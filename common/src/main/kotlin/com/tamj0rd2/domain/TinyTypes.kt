package com.tamj0rd2.domain

import dev.forkhandles.values.*
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PlayerId(override val value: String) : Value<String> {
    companion object {
        val unidentified = PlayerId("unidentified")
    }

    override fun toString(): String = value
}

@JvmInline
@Serializable
value class Bid private constructor(override val value: Int) : Value<Int> {
    companion object : IntValueFactory<Bid>(::Bid, 0.minValue)
}

@JvmInline
@Serializable
value class RoundNumber private constructor(override val value: Int) : Value<Int> {
    // TODO: this is 0 for backward compatability. But it should be 1
    companion object : IntValueFactory<RoundNumber>(::RoundNumber, 0.minValue.and(10.maxValue)) {
        val None = RoundNumber(0)
    }

    operator fun plus(increment: Int): RoundNumber {
        return RoundNumber(value + increment)
    }

    operator fun minus(decrement: Int): RoundNumber {
        return RoundNumber(value - decrement)
    }
}

@JvmInline
@Serializable
value class TrickNumber private constructor(override val value: Int) : Value<Int> {
    // TODO: this is 0 for backward compatability. But it should be 1
    companion object : IntValueFactory<TrickNumber>(::TrickNumber, 0.minValue.and(10.maxValue)) {
        val None = TrickNumber(0)
    }

    operator fun plus(increment: Int): TrickNumber {
        return TrickNumber(value + increment)
    }

    operator fun minus(decrement: Int): TrickNumber {
        return TrickNumber(value - decrement)
    }

    operator fun compareTo(other: Int): Int {
        return value.compareTo(other)
    }
}
