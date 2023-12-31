package com.tamj0rd2.domain

import kotlinx.serialization.Serializable

data class GameState(
    val trickNumber: TrickNumber,
    val roundNumber: RoundNumber,
    val state: GamePhase,
    val phase: RoundPhase?,
    val players: List<PlayerId>,
    val hands: Map<PlayerId, List<Card>>,
    val riggedHands: Map<PlayerId, List<Card>>,
    val bids: Map<PlayerId, Bid>,
    val trick: Trick?,
    val roundTurnOrder: List<PlayerId>,
)

@Serializable
enum class GamePhase {
    WaitingForMorePlayers,
    WaitingToStart,
    InProgress,
    Complete,
    ;

    companion object {
        private val mapperByName = entries.associateBy { it.name }
        fun from(state: String) = mapperByName[state] ?: error("unknown state $state")
    }
}

@Serializable
enum class RoundPhase {
    Bidding,
    BiddingCompleted,
    TrickTaking,
    TrickCompleted;

    companion object {
        private val mapper = entries.associateBy { it.name }
        fun from(phase: String) = mapper[phase] ?: error("unknown phase $phase")
    }
}

@Serializable
sealed class DisplayBid {
    override fun toString(): String = when (this) {
        is None -> "None"
        is Hidden -> "Hidden"
        is Placed -> bid.toString()
    }

    @Serializable
    object None : DisplayBid()

    @Serializable
    object Hidden : DisplayBid()

    @Serializable
    data class Placed(val bid: Bid) : DisplayBid()

    companion object {
        fun parse(text: String): DisplayBid {
            return when (text) {
                "None" -> None
                "Hidden" -> Hidden
                else -> Placed(Bid.parse(text))
            }
        }
    }
}