package com.tamj0rd2.webapp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addMixIn
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.Command
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.core.Body
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.int
import org.http4k.format.text
import org.http4k.format.uuid
import org.http4k.format.withStandardMappings
import org.http4k.websocket.WsMessage

object CustomJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(::PlayerId, PlayerId::playerId)
        .int(::Bid, Bid::bid)
        .uuid(::MessageId, MessageId::value)
        .done()
        .addMixIn<Notification, DefaultMixin>()
        .addMixIn<Message, DefaultMixin>()
        .addMixIn<Card, DefaultMixin>()
        .addMixIn<Command, DefaultMixin>()
)

internal val messageLens = WsMessage.auto<Message>().toLens()
internal val gameMasterCommandLens = Body.auto<Command.GameMasterCommand>().toLens()

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
private abstract class DefaultMixin
