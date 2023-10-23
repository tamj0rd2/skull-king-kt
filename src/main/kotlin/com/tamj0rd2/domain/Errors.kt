package com.tamj0rd2.domain

sealed class GameException(message: String) : Exception(message) {
    class CannotJoinGame(reason: String? = "no reason provided") : GameException("cannot join game: $reason")
    class CannotBid(reason: String? = "no reason provided") : GameException("cannot bid: $reason")
    class CannotPlayCard(reason: String? = "no reason provided") : GameException("cannot play card: $reason")
}
