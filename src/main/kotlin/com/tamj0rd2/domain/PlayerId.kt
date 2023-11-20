package com.tamj0rd2.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// TODO: Do I really have to clutter the domain with serialization annotations for kotlinx?
@Serializable(with = PlayerIdSerializer::class)
// use a custom serialization until I've removed the non-svelte version of the FE, then change PlayerId to a value class
// https://github.com/adamko-dev/kotlinx-serialization-typescript-generator/blob/main/docs/maps.md#maps-with-complex-keys---map-key-class
data class PlayerId(val playerId: String) {
    override fun toString(): String = playerId

    companion object {
        val unidentified = PlayerId("unidentified")
    }
}

// TODO: it pains me that this needs to be part of the domain... At least it'll be gone soon
object PlayerIdSerializer : KSerializer<PlayerId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PlayerId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PlayerId) {
        encoder.encodeString(value.playerId)
    }

    override fun deserialize(decoder: Decoder): PlayerId {
        return PlayerId(decoder.decodeString())
    }
}
