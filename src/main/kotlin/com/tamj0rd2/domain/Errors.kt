package com.tamj0rd2.domain

sealed class GameException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PlayerWithSameNameAlreadyJoined(playerId: PlayerId) :
        GameException("a player with the name $playerId is already in the game")

    class CannotBid(reason: String? = null) : GameException(reason?.let { "cannot bid: $reason" } ?: "cannot bid")
}
