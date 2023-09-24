package com.tamj0rd2.webapp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addMixIn
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.format.withStandardMappings

object CustomJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(::PlayerId, PlayerId::playerId)
        .done()
        .addMixIn<MessageToClient, DefaultMixin>()
        .addMixIn<GameMasterCommand, DefaultMixin>()
        .addMixIn<MessageFromClient, DefaultMixin>()
        .addMixIn<Card, DefaultMixin>()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
private abstract class DefaultMixin
