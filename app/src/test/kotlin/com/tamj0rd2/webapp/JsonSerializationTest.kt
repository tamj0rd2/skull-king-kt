package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.blue
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.messaging.Notification
import com.tamj0rd2.webapp.CustomJsonSerializer.asA
import com.tamj0rd2.webapp.CustomJsonSerializer.asCompactJsonString
import com.tamj0rd2.webapp.CustomJsonSerializer.asJsonObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JsonSerializationTest {
    @Test
    fun `can serialize PlayerIds`() {
        test(PlayerId("somePlayer"), "\"somePlayer\"")
    }

    @Test
    fun `can serialize Bids`() {
        test(Bid.of(1), "1")
    }

    @Test
    fun `can serialize and deserialize Notifications`() {
        test<Notification>(
            Notification.BiddingCompleted(
                mapOf(
                    PlayerId("somePlayer") to Bid.of(1),
                    PlayerId("someOtherPlayer") to Bid.of(2)
                )
            ),
            """{"type":"com.tamj0rd2.messaging.Notification.BiddingCompleted","bids":{"somePlayer":1,"someOtherPlayer":2}}"""
        )
    }

    @Test
    fun `can serialize and deserialize GameMasterCommands`() {
        test<GameMasterCommand>(
            GameMasterCommand.RigDeck(PlayerId("somePlayer"), listOf(11.blue)),
            """{"type":"com.tamj0rd2.domain.GameMasterCommand.RigDeck","playerId":"somePlayer","cards":[{"type":"com.tamj0rd2.domain.Card.NumberedCard","name":"Blue-11","suit":"Blue","number":11}]}"""
        )
    }

    @Test
    fun `can serialize and deserialize PlayerCommands`() {
        test<PlayerCommand>(
            PlayerCommand.JoinGame(PlayerId("somePlayer")),
            """{"type":"com.tamj0rd2.domain.PlayerCommand.JoinGame","actor":"somePlayer"}"""
        )
    }

    @Test
    fun `can serialize and deserialize Messages`() {
        val obj = Message.ToServer(PlayerCommand.JoinGame(PlayerId("freddy_first")))
        test<Message>(
            obj,
            """{"type":"com.tamj0rd2.messaging.Message.ToServer","command":{"type":"com.tamj0rd2.domain.PlayerCommand.JoinGame","actor":"freddy_first"},"id":"${obj.id}"}"""
        )
    }

    private inline fun <reified T : Any> test(obj: T, expectedJson: String) {
        val normalKotlinX = Json {
            ignoreUnknownKeys = true
        }

        val normalKotlinxJson = normalKotlinX.encodeToString(obj)
        println("normal kotlinx: $normalKotlinxJson")
        normalKotlinxJson shouldBe expectedJson

        val customKotlinxJson = obj.asJsonObject().asCompactJsonString()
        println("custom serialization: $customKotlinxJson")
        customKotlinxJson shouldBe expectedJson

        withClue("serialized using normal kotlinx") {
            withClue("normal kotlinx deserialization failed") {
                normalKotlinX.decodeFromString<T>(normalKotlinxJson) shouldBe obj
            }
            withClue("custom deserialization failed") {
                normalKotlinxJson.asA(T::class) shouldBe obj
            }
        }

        withClue("serialized using custom serialization") {
            withClue("custom deserialization failed") {
                customKotlinxJson.asA(T::class) shouldBe obj
            }
            withClue("normal kotlinx deserialization failed") {
                normalKotlinX.decodeFromString<T>(customKotlinxJson) shouldBe obj
            }
        }
    }
}
