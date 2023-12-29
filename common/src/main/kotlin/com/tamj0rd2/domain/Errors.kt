package com.tamj0rd2.domain

sealed class CommandError {
    abstract val reason: GameErrorCode

    data class FailedToPlayCard(
        val playerId: PlayerId,
        val card: Card,
        override val reason: GameErrorCode,
        val trick: List<PlayedCard>,
        val hand: List<Card>,
    ) : CommandError()
}

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

    fun throwException(): Nothing = throw asException()
    fun asException(): GameErrorCodeException = GameErrorCodeException(this)

    companion object {
        private val mapper = entries.associateBy { it.name }
        fun fromString(name: String): GameErrorCode =
            mapper[name] ?: throw IllegalArgumentException("No GameErrorCode with name '$name'")
    }
}

data class GameErrorCodeException(val errorCode: GameErrorCode) : Exception(errorCode.name) {
    override fun toString(): String {
        return errorCode.name
    }
}
