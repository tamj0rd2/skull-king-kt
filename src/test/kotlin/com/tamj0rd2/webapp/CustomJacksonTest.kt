package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.blue
import com.tamj0rd2.webapp.CustomJackson.asA
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CustomJacksonTest {
    @Test
    fun `can serialize player ids`() {
        val expected = PlayerId("somePlayer")
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(PlayerId::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize messages to the client`() {
        val expected = MessageToClient.BidPlaced(PlayerId("somePlayer"))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)
        jsonString shouldBe """{"type":"MessageToClient${"$"}BidPlaced","playerId":"somePlayer"}"""

        val result = jsonString.asJsonObject().asA(MessageToClient::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize messages to the client - sanity check`() {
        val expected = MessageToClient.BiddingCompleted(mapOf(PlayerId("somePlayer") to 1, PlayerId("someOtherPlayer") to 2))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)
        jsonString.replace("$", "_") shouldBe """{"type":"MessageToClient_BiddingCompleted","bids":{"somePlayer":1,"someOtherPlayer":2}}"""

        val result = jsonString.asJsonObject().asA(MessageToClient::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize GameMasterCommands`() {
        val expected = GameMasterCommand.RigDeck(PlayerId("somePlayer"), listOf(11.blue))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(GameMasterCommand::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize ClientMessages`() {
        val expected = MessageFromClient.BidPlaced(1)
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(MessageFromClient::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize Cards`() {
        val expected = 13.blue
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(Card::class)
        result shouldBe expected
    }
}
