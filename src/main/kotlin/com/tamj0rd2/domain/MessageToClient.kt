package com.tamj0rd2.domain

sealed class MessageToClient {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : MessageToClient()

    data class GameStarted(val players: List<PlayerId>) : MessageToClient()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : MessageToClient()

    data class BidPlaced(val playerId: PlayerId) : MessageToClient()

    data class BiddingCompleted(val bids: Map<PlayerId, Int>) : MessageToClient()

    data class CardPlayed(val playerId: PlayerId, val card: Card, val nextPlayer: PlayerId?) : MessageToClient()

    data class TrickStarted(val trickNumber: Int, val firstPlayer: PlayerId) : MessageToClient()

    object TrickCompleted : MessageToClient()
    object GameCompleted : MessageToClient()
}
