package com.tamj0rd2.domain

import com.tamj0rd2.domain.Card.NumberedCard
import com.tamj0rd2.domain.Card.SpecialCard
import com.tamj0rd2.domain.SpecialSuit.*
import com.tamj0rd2.domain.Suit.Black
import kotlinx.serialization.Serializable

@Serializable
data class Trick(
    val size: Int,
    val playedCards: List<PlayedCard>,
) {
    companion object {
        val None = ofSize(0)

        fun ofSize(size: Int) = Trick(
            size = size,
            playedCards = emptyList(),
        )
    }

    val suit: Suit? = playedCards.asSequence().map { it.card }.filterIsInstance<NumberedCard>().firstOrNull()?.suit

    val isComplete get() = playedCards.size == size

    fun add(playedCard: PlayedCard) = copy(playedCards = playedCards + playedCard)

    fun isCardPlayable(card: Card, restOfHand: List<Card>): Boolean {
        if (restOfHand.isEmpty() || suit == null) return true

        return when (card) {
            is NumberedCard -> {
                if (card.suit == suit) true
                else restOfHand.none { it is NumberedCard && it.suit == suit }
            }

            is SpecialCard -> true
        }
    }

    fun suitIs(suit: Suit) = this.suit == suit

    private val hasSkullKing = playedCards.any { it.card == SpecialCard.skullKing }
    private val hasMermaid = playedCards.any { it.card == SpecialCard.mermaid }
    private val hasPirate = playedCards.any { it.card == SpecialCard.pirate }

    val winner: PlayerId
        get() {
            require(isComplete) { "trick is not complete" }

            val winner = playedCards.reduce { winnerSoFar, playedCard ->
                if (playedCard.card.beats(winnerSoFar.card)) {
                    return@reduce playedCard
                }

                if (hasMermaid && hasSkullKing) {
                    return@reduce playedCards.first { it.card == SpecialCard.mermaid }
                }

                winnerSoFar
            }

            return winner.playerId
        }

    private fun Card.beats(other: Card): Boolean {
        return when (this) {
            is NumberedCard -> beats(other)
            is SpecialCard -> beats(other)
        }
    }

    private fun NumberedCard.beats(other: Card): Boolean {
        when (other) {
            is NumberedCard -> {
                val areEitherBlack = suit == Black || other.suit == Black

                if (areEitherBlack && !suitIs(Black)) {
                    if (suit == other.suit) return number > other.number
                    return suit == Black
                }

                if (suitIs(suit)) return number > other.number
                return false
            }

            is SpecialCard -> return when (other.suit) {
                Escape -> true
                Pirate, Mermaid, SkullKing -> false
            }
        }
    }

    private fun SpecialCard.beats(other: Card): Boolean {
        return when (other) {
            is NumberedCard -> suit != Escape
            is SpecialCard -> {
                if (suit == other.suit) return false

                when (suit) {
                    Escape -> false
                    Pirate -> !hasSkullKing
                    Mermaid -> hasSkullKing || !hasPirate
                    SkullKing -> !hasMermaid
                }
            }
        }
    }
}
