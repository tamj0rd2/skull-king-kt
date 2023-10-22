package com.tamj0rd2.webapp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addMixIn
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.int
import org.http4k.format.text
import org.http4k.format.withStandardMappings
import org.http4k.websocket.WsMessage

object CustomJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(::PlayerId, PlayerId::playerId)
        .int(::Bid, Bid::bid)
        .done()
        .addMixIn<MessageToClient, DefaultMixin>()
        .addMixIn<GameMasterCommand, DefaultMixin>()
        .addMixIn<ClientMessage, DefaultMixin>()
        .addMixIn<OverTheWireMessage, DefaultMixin>()
        .addMixIn<Card, DefaultMixin>()
)

internal val overTheWireMessageLens = WsMessage.auto<OverTheWireMessage>().toLens()

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
private abstract class DefaultMixin
