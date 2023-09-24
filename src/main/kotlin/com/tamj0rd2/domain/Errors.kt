package com.tamj0rd2.domain

sealed class GameException(message: String) : Exception(message) {
    class PlayerWithSameNameAlreadyJoined(playerId: PlayerId) :
        GameException("a player with the name $playerId is already in the game")

    class CannotBid(reason: String? = "no reason provided") : GameException("cannot bid: $reason")
    class CannotPlayCard(reason: String? = "no reason provided") : GameException("cannot play card: $reason")
}
