package com.tamj0rd2.domain

import arrow.optics.copy
import arrow.optics.optics
import com.tamj0rd2.domain.GameState.WaitingForMorePlayers
import com.tamj0rd2.domain.GameState.WaitingToStart
import com.tamj0rd2.domain.Trick.Companion.isCardPlayable
import kotlinx.serialization.Serializable

@optics
@Serializable
data class PlayerGameState(
    val playerId: PlayerId,
    val winsOfTheRound: Map<PlayerId, Int> = emptyMap(),
    val trickWinner: PlayerId? = null,
    val currentPlayer: PlayerId? = null,
    val trickNumber: TrickNumber = TrickNumber.None,
    val roundNumber: RoundNumber = RoundNumber.None,
    val trick: List<PlayedCard> = emptyList(),
    val roundPhase: RoundPhase? = null,
    val gameState: GameState? = null,
    val playersInRoom: List<PlayerId> = emptyList(),
    val hand: List<CardWithPlayability> = emptyList(),
    val bids: Map<PlayerId, DisplayBid> = emptyMap(),
    val turnOrder: List<PlayerId> = emptyList(),
    val currentSuit: Suit? = null,
) {
    fun handle(event: GameEvent): PlayerGameState = when (event) {
        // Note: I do keep thinking that it's pointless to have these copy functions via optics. BUT
        // they are nice for conditionally updating values. Like in the example of CardPlayed. You can
        // do it with a regular copy too, but this just seems a bit nicer to me. Despite having to start
        // with the class name each time...

        is GameEvent.BidPlaced -> copy {
            PlayerGameState.bids set bids + (event.playerId to DisplayBid.Hidden)
        }

        is GameEvent.BiddingCompleted -> copy {
            PlayerGameState.roundPhase set RoundPhase.BiddingCompleted
            PlayerGameState.bids set event.bids.mapValues { DisplayBid.Placed(it.value) }
        }

        is GameEvent.CardPlayed -> copy {
            val trick = trick + event.card.playedBy(event.playerId)
            PlayerGameState.trick set trick

            val nextPlayer = turnOrder.getOrNull(trick.size)
            PlayerGameState.nullableCurrentPlayer set nextPlayer

            val currentSuit = currentSuit ?: event.card.suit
            PlayerGameState.nullableCurrentSuit set currentSuit

            PlayerGameState.hand set when (playerId) {
                event.playerId -> {
                    val cardToRemove = hand.first { it.card == event.card }
                    hand.toMutableList().apply { remove(cardToRemove) }.map { it.card.notPlayable() }
                }
                nextPlayer -> {
                    val cards = hand.map(CardWithPlayability::card)
                    hand.map {
                        val isPlayable = isCardPlayable(it.card, cards, currentSuit)
                        it.card.playable(isPlayable)
                    }
                }
                else -> hand.map { it.card.notPlayable() }
            }
        }

        is GameEvent.CardsDealt -> copy {
            val hand = event.cards[playerId] ?: error("$playerId wasn't dealt any cards")
            PlayerGameState.hand set hand.map(Card::notPlayable)
        }

        is GameEvent.GameCompleted -> copy {
            PlayerGameState.gameState set GameState.Complete
        }

        is GameEvent.GameStarted -> copy {
            PlayerGameState.gameState set GameState.InProgress
        }

        is GameEvent.PlayerJoined -> copy {
            val playersInRoom = playersInRoom + event.playerId
            PlayerGameState.playersInRoom set playersInRoom
            PlayerGameState.gameState set (if (playersInRoom.size >= minimumNumOfPlayersToStartGame) WaitingToStart else WaitingForMorePlayers)
        }

        is GameEvent.RoundStarted -> copy {
            PlayerGameState.roundNumber set roundNumber + 1
            PlayerGameState.trickNumber set TrickNumber.None
            PlayerGameState.bids set playersInRoom.associateWith { DisplayBid.None }
            PlayerGameState.roundPhase set RoundPhase.Bidding
            PlayerGameState.winsOfTheRound set playersInRoom.associateWith { 0 }
        }

        is GameEvent.TrickCompleted -> copy {
            PlayerGameState.roundPhase set RoundPhase.TrickCompleted
            PlayerGameState.trickWinner set event.winner
            PlayerGameState.winsOfTheRound set winsOfTheRound + (event.winner to (winsOfTheRound[event.winner]!! + 1))
        }

        is GameEvent.TrickStarted -> copy {
            PlayerGameState.nullableCurrentSuit set null
            PlayerGameState.trickNumber set trickNumber + 1
            PlayerGameState.trick set emptyList()
            PlayerGameState.roundPhase set RoundPhase.TrickTaking
            PlayerGameState.currentPlayer set event.turnOrder.first()
            PlayerGameState.turnOrder set event.turnOrder

            if (playerId == event.turnOrder.first()) {
                PlayerGameState.hand set hand.map { it.card.playable() }
            }
        }

        // TODO: remove SuitEstablished event
        is GameEvent.SuitEstablished -> copy {
            PlayerGameState.currentSuit set event.suit

            //val handWithoutPlayability = hand.map(CardWithPlayability::card)
            //val hand = hand.map {
            //    val isPlayable = isCardPlayable(it.card, handWithoutPlayability, event.suit)
            //    it.card.playable(isPlayable)
            //}
            //PlayerGameState.hand set hand
        }
    }

    private val Card.suit get() = if (this is Card.NumberedCard) suit else null

    fun handle(events: List<GameEvent>): PlayerGameState =
        events.fold(this) { state, event -> state.handle(event) }

    companion object {
        private const val minimumNumOfPlayersToStartGame = 2

        fun ofPlayer(playerId: PlayerId, allEventsSoFar: List<GameEvent>): PlayerGameState {
            val initial = PlayerGameState(playerId)
            return allEventsSoFar.fold(initial) { state, event -> state.handle(event) }
        }
    }
}
