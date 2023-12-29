package com.tamj0rd2.domain

import arrow.optics.copy
import arrow.optics.optics
import com.tamj0rd2.domain.GameState.WaitingForMorePlayers
import com.tamj0rd2.domain.GameState.WaitingToStart

@optics
data class PlayerGameState(
    val playerId: PlayerId,
    val winsOfTheRound: Map<PlayerId, Int>,
    val trickWinner: PlayerId?,
    val currentPlayer: PlayerId?,
    val trickNumber: TrickNumber,
    val roundNumber: RoundNumber,
    val trick: List<PlayedCard>,
    val roundPhase: RoundPhase?,
    val gameState: GameState?,
    val playersInRoom: List<PlayerId>,
    val hand: List<CardWithPlayability>,
    val bids: Map<PlayerId, DisplayBid>,
    val turnOrder: List<PlayerId>,
) {
    fun handle(event: GameEvent): PlayerGameState = when (event) {
        is GameEvent.BidPlaced -> copy {
            PlayerGameState.bids set bids + (event.playerId to DisplayBid.Hidden)
        }

        is GameEvent.BiddingCompleted -> copy {
            PlayerGameState.roundPhase set RoundPhase.BiddingCompleted
        }

        is GameEvent.CardPlayed -> copy {
            if (event.playerId == playerId) {
                val cardToRemoveFromHand = hand.first { it.card == event.card }
                PlayerGameState.hand set hand.toMutableList().apply { remove(cardToRemoveFromHand) }
            }

            val updatedTrick = trick + event.card.playedBy(event.playerId)
            PlayerGameState.trick set updatedTrick
            PlayerGameState.nullableCurrentPlayer set turnOrder[updatedTrick.size]
        }

        is GameEvent.CardsDealt -> copy {
            val cards = event.cards[playerId] ?: error("$playerId wasn't dealt any cards")
            PlayerGameState.hand set cards.map(Card::playable)
        }

        is GameEvent.GameCompleted -> TODO()
        is GameEvent.GameStarted -> copy {
            PlayerGameState.gameState set GameState.InProgress
        }

        is GameEvent.PlayerJoined -> copy {
            PlayerGameState.playersInRoom transform { players -> players + event.playerId }
            PlayerGameState.gameState set (if (playersInRoom.size >= minimumNumOfPlayersToStartGame) WaitingToStart else WaitingForMorePlayers)
        }

        is GameEvent.RoundStarted -> copy {
            PlayerGameState.roundNumber set roundNumber + 1
            PlayerGameState.trickNumber set TrickNumber.None
            PlayerGameState.bids set playersInRoom.associateWith { DisplayBid.None }
            PlayerGameState.roundPhase set RoundPhase.Bidding
            PlayerGameState.winsOfTheRound set playersInRoom.associateWith { 0 }
        }

        is GameEvent.TrickCompleted -> TODO()
        is GameEvent.TrickStarted -> copy {
            PlayerGameState.trickNumber set trickNumber + 1
            PlayerGameState.trick set emptyList()
            PlayerGameState.roundPhase set RoundPhase.TrickTaking
            PlayerGameState.currentPlayer set event.turnOrder.first()
            PlayerGameState.turnOrder set event.turnOrder
        }
    }

    fun handle(events: List<GameEvent>): PlayerGameState =
        events.fold(this) { state, event -> state.handle(event) }

    companion object {
        const val minimumNumOfPlayersToStartGame = 2

        fun ofPlayer(player: PlayerId, allEventsSoFar: List<GameEvent>): PlayerGameState {
            val initial = PlayerGameState(
                playerId = player,
                winsOfTheRound = emptyMap(),
                trickWinner = null,
                currentPlayer = null,
                trickNumber = TrickNumber.None,
                roundNumber = RoundNumber.None,
                trick = emptyList(),
                roundPhase = null,
                gameState = null,
                playersInRoom = emptyList(),
                hand = emptyList(),
                bids = emptyMap(),
                turnOrder = emptyList(),
            )

            return allEventsSoFar.fold(initial) { state, event -> state.handle(event) }
        }
    }
}
