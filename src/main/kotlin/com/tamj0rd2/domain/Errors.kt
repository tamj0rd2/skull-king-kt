package com.tamj0rd2.domain

import java.lang.Exception

sealed class GameException(message: String) : Exception(message) {
    class NotEnoughPlayers(playerCount: Int, requiredCount: Int) : GameException("$playerCount/$requiredCount players isn't enough to start the game")
    class NoHandFoundFor(playerId: PlayerId) : GameException("no hand found for player $playerId")
    class CardNotInHand(playerId: PlayerId, cardId: CardId, cardsInHand: List<Card>) : GameException("card $cardId not in $playerId's hand: $cardsInHand")
    class NotAllPlayersHaveBid : GameException("not all players have bid")
    class NotStarted : GameException("game not started")
}