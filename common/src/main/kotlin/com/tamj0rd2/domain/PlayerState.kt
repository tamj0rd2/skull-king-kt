package com.tamj0rd2.domain

import arrow.optics.copy
import arrow.optics.optics
import com.tamj0rd2.domain.GamePhase.WaitingForMorePlayers
import com.tamj0rd2.domain.GamePhase.WaitingToStart
import kotlinx.serialization.Serializable

@optics
@Serializable
data class PlayerState(
    val playerId: PlayerId,
    val winsOfTheRound: Map<PlayerId, Int> = emptyMap(),
    val trickWinner: PlayerId? = null,
    val currentPlayer: PlayerId? = null,
    val trickNumber: TrickNumber = TrickNumber.None,
    val roundNumber: RoundNumber = RoundNumber.None,
    val trick: Trick? = null,
    val roundPhase: RoundPhase? = null,
    val gamePhase: GamePhase? = null,
    val playersInRoom: List<PlayerId> = emptyList(),
    val hand: List<CardWithPlayability> = emptyList(),
    val bids: Map<PlayerId, DisplayBid> = emptyMap(),
    val turnOrder: List<PlayerId> = emptyList(),
    val currentSuit: Suit? = null,
) {
    fun handle(event: GameEvent): PlayerState = when (event) {
        // Note: I do keep thinking that it's pointless to have these copy functions via optics. BUT
        // they are nice for conditionally updating values. Like in the example of CardPlayed. You can
        // do it with a regular copy too, but this just seems a bit nicer to me. Despite having to start
        // with the class name each time...

        is GameEvent.BidPlaced -> copy {
            PlayerState.bids set bids + (event.playerId to DisplayBid.Hidden)
        }

        is GameEvent.BiddingCompleted -> copy {
            PlayerState.roundPhase set RoundPhase.BiddingCompleted
            PlayerState.bids set event.bids.mapValues { DisplayBid.Placed(it.value) }
        }

        is GameEvent.CardPlayed -> copy {
            val trick = trick!!.add(event.card.playedBy(event.playerId))
            PlayerState.trick set trick

            val nextPlayer = turnOrder.getOrNull(trick.playedCards.size)
            PlayerState.nullableCurrentPlayer set nextPlayer

            val currentSuit = currentSuit ?: event.card.suit
            PlayerState.nullableCurrentSuit set currentSuit

            PlayerState.hand set when (playerId) {
                event.playerId -> {
                    val cardToRemove = hand.first { it.card == event.card }
                    hand.toMutableList().apply { remove(cardToRemove) }.map { it.card.notPlayable() }
                }

                nextPlayer -> {
                    val cards = hand.map(CardWithPlayability::card)
                    hand.map {
                        val isPlayable = trick.isCardPlayable(it.card, cards)
                        it.card.playable(isPlayable)
                    }
                }

                else -> hand.map { it.card.notPlayable() }
            }
        }

        is GameEvent.CardsDealt -> copy {
            val hand = event.cards[playerId] ?: error("$playerId wasn't dealt any cards")
            PlayerState.hand set hand.map(Card::notPlayable)
        }

        is GameEvent.GameCompleted -> copy {
            PlayerState.gamePhase set GamePhase.Complete
        }

        is GameEvent.GameStarted -> copy {
            PlayerState.gamePhase set GamePhase.InProgress
        }

        is GameEvent.PlayerJoined -> copy {
            val playersInRoom = playersInRoom + event.playerId
            PlayerState.playersInRoom set playersInRoom
            PlayerState.gamePhase set (if (playersInRoom.size >= minimumNumOfPlayersToStartGame) WaitingToStart else WaitingForMorePlayers)
        }

        is GameEvent.RoundStarted -> copy {
            PlayerState.roundNumber set roundNumber + 1
            PlayerState.trickNumber set TrickNumber.None
            PlayerState.bids set playersInRoom.associateWith { DisplayBid.None }
            PlayerState.roundPhase set RoundPhase.Bidding
            PlayerState.winsOfTheRound set playersInRoom.associateWith { 0 }
        }

        is GameEvent.TrickCompleted -> copy {
            PlayerState.roundPhase set RoundPhase.TrickCompleted
            PlayerState.trickWinner set event.winner
            PlayerState.winsOfTheRound set winsOfTheRound + (event.winner to (winsOfTheRound[event.winner]!! + 1))
        }

        is GameEvent.TrickStarted -> copy {
            PlayerState.nullableCurrentSuit set null
            PlayerState.trickNumber set trickNumber + 1
            PlayerState.trick set Trick.ofSize(playersInRoom.size)
            PlayerState.roundPhase set RoundPhase.TrickTaking
            PlayerState.currentPlayer set event.turnOrder.first()
            PlayerState.turnOrder set event.turnOrder

            if (playerId == event.turnOrder.first()) {
                PlayerState.hand set hand.map { it.card.playable() }
            }
        }
    }

    private val Card.suit get() = if (this is Card.NumberedCard) suit else null

    fun handle(events: List<GameEvent>): PlayerState =
        events.fold(this) { state, event -> state.handle(event) }

    companion object {
        private const val minimumNumOfPlayersToStartGame = 2

        fun ofPlayer(playerId: PlayerId, allEventsSoFar: List<GameEvent>): PlayerState {
            val initial = PlayerState(playerId)
            return allEventsSoFar.fold(initial) { state, event -> state.handle(event) }
        }
    }
}
