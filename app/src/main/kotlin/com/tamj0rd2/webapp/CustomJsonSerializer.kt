package com.tamj0rd2.webapp

import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.webapp.CustomJsonSerializer.auto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.http4k.core.Body
import org.http4k.format.ConfigurableKotlinxSerialization
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.websocket.WsMessage

object CustomJsonSerializer : ConfigurableKotlinxSerialization({
    ignoreUnknownKeys = true
    encodeDefaults = true
    asConfigurable().withStandardMappings().done()
}) {
    override fun asJsonObject(input: Any): JsonElement = when (input) {
        is Map<*, *> -> JsonObject(
            input.mapNotNull {
                (it.key as? String ?: return@mapNotNull null) to (it.value?.asJsonObject() ?: nullNode())
            }.toMap(),
        )

        is Iterable<*> -> JsonArray(input.map { it?.asJsonObject() ?: nullNode() })
        is Array<*> -> JsonArray(input.map { it?.asJsonObject() ?: nullNode() })
        else -> {
            val javaClass = input::class.java.let {
                if (it.superclass.isSealed && !it.superclass.isPrimitive) it.superclass else it
            }
            json.encodeToJsonElement(json.serializersModule.serializer(javaClass), input)
        }
    }
}

internal val messageLens = WsMessage.auto<Message>().toLens()
internal val gameMasterCommandLens = Body.auto<GameMasterCommand>().toLens()
