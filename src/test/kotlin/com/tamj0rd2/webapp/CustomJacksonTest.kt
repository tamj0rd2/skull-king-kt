package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.blue
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asA
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomJacksonTest {
    @Test
    fun `can serialize and deserialize game events`() {
        val expected = MessageToClient.BidPlaced("somePlayer")
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(MessageToClient::class)
        assertEquals(expected, result)
    }

    @Test
    fun `can serialize and deserialize GameMasterCommands`() {
        val expected = GameMasterCommand.RigDeck("somePlayer", listOf(11.blue))
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(GameMasterCommand::class)
        assertEquals(expected, result)
    }

    @Test
    fun `can serialize and deserialize ClientMessages`() {
        val expected = MessageFromClient.BidPlaced(1)
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(MessageFromClient::class)
        assertEquals(expected, result)
    }

    @Test
    fun `can serialize and deserialize Cards`() {
        val expected = 13.blue
        val jsonString = expected.asJsonObject().asCompactJsonString()
        println(jsonString)

        val result = jsonString.asJsonObject().asA(Card::class)
        assertEquals(expected, result)
    }
}
