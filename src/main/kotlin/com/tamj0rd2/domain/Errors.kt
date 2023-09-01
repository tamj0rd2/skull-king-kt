package com.tamj0rd2.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.lang.Exception

sealed class GameException(override val message: String) : Exception(message) {
    val errorCode: GameErrorCode
        get() = when (this) {
            is NotStarted -> GameErrorCode.NotStarted
            else -> throw IllegalStateException("could not create an error code for $this")
        }

    class NotEnoughPlayers(playerCount: Int, requiredCount: Int) : GameException("$playerCount/$requiredCount players isn't enough to start the game")
    class NoHandFoundFor(playerId: PlayerId) : GameException("no hand found for player $playerId")
    class CardNotInHand(playerId: PlayerId, cardId: CardId) : GameException("card $cardId not in $playerId's hand")
    class NotAllPlayersHaveBid : GameException("not all players have bid")
    class NotStarted : GameException("game not started")
    class PlayerWithSameNameAlreadyJoined(playerId: PlayerId) : GameException("a player with the name $playerId is already in the game")
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class GameErrorCode {
    override fun toString() = this::class.simpleName ?: error("this should never fail")
    object NotStarted : GameErrorCode()
    companion object {
        fun from(type: String): GameErrorCode {
            when (type) {
                "GameErrorCode\$NotStarted" -> return NotStarted
                else -> throw IllegalArgumentException("could not create a GameErrorCode from '$type'")
            }
        }
    }
}
