package com.tamj0rd2.domain

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Bid(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }
}
