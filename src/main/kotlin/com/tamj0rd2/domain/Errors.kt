package com.tamj0rd2.domain

enum class GameErrorCode {
    PlayerWithSameNameAlreadyInGame,
    ;

    fun throwException(): Nothing = throw GameErrorCodeException(this)

    companion object {
        private val mapper = values().associateBy { it.name }
        fun fromString(name: String): GameErrorCode = mapper[name] ?: throw IllegalArgumentException("No GameErrorCode with name $name")
    }
}

data class GameErrorCodeException(val errorCode: GameErrorCode) : Exception(errorCode.name)

sealed class GameException(message: String) : Exception(message) {
    class CannotBid(reason: String? = "no reason provided") : GameException("cannot bid: $reason")
    class CannotPlayCard(reason: String? = "no reason provided") : GameException("cannot play card: $reason")
}
