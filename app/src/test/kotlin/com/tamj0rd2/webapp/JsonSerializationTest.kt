package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerGameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.blue
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.webapp.CustomJsonSerializer.asA
import com.tamj0rd2.webapp.CustomJsonSerializer.asCompactJsonString
import com.tamj0rd2.webapp.CustomJsonSerializer.asJsonObject
import io.kotest.assertions.json.shouldEqualJson
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
    fun `can serialize and deserialize GameMasterCommands`() {
        test<GameMasterCommand>(
            GameMasterCommand.RigDeck(PlayerId("somePlayer"), listOf(11.blue)),
            // language=JSON
            """
                {
                    "type": "com.tamj0rd2.domain.GameMasterCommand.RigDeck",
                    "playerId": "somePlayer",
                    "cards": [
                        {
                            "type": "com.tamj0rd2.domain.Card.NumberedCard",
                            "name": "Blue-11",
                            "suit": "Blue",
                            "number": 11
                        }
                    ]
                }
            """.trimIndent()
        )
    }

    @Test
    fun `can serialize and deserialize PlayerCommands`() {
        test<PlayerCommand>(
            PlayerCommand.JoinGame(PlayerId("somePlayer")),
            // language=JSON
            """
                {
                    "type": "com.tamj0rd2.domain.PlayerCommand.JoinGame",
                    "actor": "somePlayer"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `can serialize and deserialize Messages`() {
        val obj = Message.ToServer(PlayerCommand.JoinGame(PlayerId("freddy_first")))
        test<Message>(
            obj,
            // language=JSON
            """
                {
                    "type": "com.tamj0rd2.messaging.Message.ToServer",
                    "command": {
                        "type": "com.tamj0rd2.domain.PlayerCommand.JoinGame",
                        "actor": "freddy_first"
                    },
                    "id": "${obj.id}"
                }
            """.trimIndent()
        )
    }

    @Test
    fun `can serialize and deserialize player state`() {
        val playerState = PlayerGameState.ofPlayer(PlayerId("somePlayer"), emptyList())
        test<PlayerGameState>(
            playerState,
            // language=JSON
            """
            {
                "playerId": "somePlayer",
                "gameState": null,
                "playersInRoom": [],
                "roundNumber": 0,
                "trickNumber": 0,
                "roundPhase": null,
                "hand": [],
                "bids": {},
                "trick": [],
                "trickWinner": null,
                "winsOfTheRound": {},
                "turnOrder": [],
                "currentPlayer": null,
                "currentSuit": null
            }
        """.trimIndent()
        )
    }

    private inline fun <reified T : Any> test(obj: T, expectedJson: String) {
        val normalKotlinX = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val normalKotlinxJson = normalKotlinX.encodeToString(obj)
        println("normal kotlinx: $normalKotlinxJson")
        normalKotlinxJson shouldEqualJson expectedJson

        val customKotlinxJson = obj.asJsonObject().asCompactJsonString()
        println("custom serialization: $customKotlinxJson")
        customKotlinxJson shouldEqualJson expectedJson

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
