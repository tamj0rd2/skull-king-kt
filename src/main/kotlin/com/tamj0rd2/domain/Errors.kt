package com.tamj0rd2.domain

enum class GameErrorCode {
    PlayerWithSameNameAlreadyInGame,
    GameNotInProgress,
    TrickNotInProgress,
    BiddingIsNotInProgress,
    PlayingCardWouldBreakSuitRules,
    NotYourTurn,
    AlreadyPlacedABid,
    BidLessThan0OrGreaterThanRoundNumber,
    ;

    fun throwException(): Nothing = throw GameErrorCodeException(this)

    companion object {
        private val mapper = values().associateBy { it.name }
        fun fromString(name: String): GameErrorCode = mapper[name] ?: throw IllegalArgumentException("No GameErrorCode with name '$name'")
    }
}

data class GameErrorCodeException(val errorCode: GameErrorCode) : Exception(errorCode.name)
