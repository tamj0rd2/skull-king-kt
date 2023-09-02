package com.tamj0rd2.domain

sealed class GameException(override val message: String) : Exception(message) {
    val errorCode: GameErrorCode
        get() = when (this) {
            is NotStarted -> GameErrorCode.NotStarted
            else -> throw IllegalStateException("could not create an error code for $this")
        }

    class NotStarted : GameException("game not started")
    class PlayerWithSameNameAlreadyJoined(playerId: PlayerId) :
        GameException("a player with the name $playerId is already in the game")
}

enum class GameErrorCode {
    NotStarted;

    companion object {
        private val entries = values().associateBy { it.name }

        fun from(type: String): GameErrorCode {
            return this.entries[type.substringAfter("GameErrorCode\$")]
                ?: throw IllegalArgumentException("could not create a GameErrorCode from '$type'")
        }
    }
}
