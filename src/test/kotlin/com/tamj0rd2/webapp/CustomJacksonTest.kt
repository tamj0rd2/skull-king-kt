package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.Command
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
        val expected = ServerMessage.BidPlaced(PlayerId("somePlayer"))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)
        jsonString shouldBe """{"type":"MessageToClient${"$"}BidPlaced","playerId":"somePlayer"}"""

        val result = jsonString.asJsonObject().asA(ServerMessage::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize messages to the client - sanity check`() {
        val expected = ServerMessage.BiddingCompleted(mapOf(PlayerId("somePlayer") to Bid(1), PlayerId("someOtherPlayer") to Bid(2)))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)
        jsonString.replace("$", "_") shouldBe """{"type":"MessageToClient_BiddingCompleted","bids":{"somePlayer":1,"someOtherPlayer":2}}"""

        val result = jsonString.asJsonObject().asA(ServerMessage::class)
        result shouldBe expected
    }

    @Test
    fun `can serialize and deserialize GameMasterCommands`() {
        val expected = Command.GameMasterCommand.RigDeck(PlayerId("somePlayer"), listOf(11.blue))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(Command::class)
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
