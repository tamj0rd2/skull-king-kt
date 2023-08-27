package com.tamj0rd2.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class GameEvent {
    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : GameEvent()

    object GameStarted : GameEvent()

    data class RoundStarted(val cardsDealt: List<Card>, val roundNumber: Int) : GameEvent() {
        // used by the FE
        val trickNumber = 1
    }

    data class BetPlaced(val playerId: PlayerId, val isBettingComplete: Boolean) : GameEvent()

    data class BettingCompleted(val bets: Map<PlayerId, Int>) : GameEvent()

    data class CardPlayed(val playerId: String, val cardId: CardId) : GameEvent()

    object TrickCompleted : GameEvent()
}