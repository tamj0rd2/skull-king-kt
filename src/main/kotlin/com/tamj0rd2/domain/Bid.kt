package com.tamj0rd2.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BidSerializer::class)
data class Bid(val bid: Int)

// TODO: it pains me that this needs to be part of the domain... At least it'll be gone soon
object BidSerializer : KSerializer<Bid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Bid", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Bid) {
        encoder.encodeInt(value.bid)
    }

    override fun deserialize(decoder: Decoder): Bid {
        return Bid(decoder.decodeInt())
    }
}
