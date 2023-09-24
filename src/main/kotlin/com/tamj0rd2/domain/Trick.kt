package com.tamj0rd2.domain

import com.tamj0rd2.domain.Card.NumberedCard
import com.tamj0rd2.domain.Card.SpecialCard
import com.tamj0rd2.domain.SpecialSuit.*
import com.tamj0rd2.domain.Suit.Black

class Trick(private val size: Int) {
    val playedCards: List<PlayedCard> get() = _playedCards
    val isComplete: Boolean get() = _playedCards.size == size

    private val _playedCards = mutableListOf<PlayedCard>()

    private var suit: Suit? = null
    private val specialsPlayed = mutableSetOf<SpecialSuit>()
    private val hasSkullKing get() = specialsPlayed.contains(SkullKing)
    private val hasMermaid get() = specialsPlayed.contains(Mermaid)
    private val hasPirate get() = specialsPlayed.contains(Pirate)

    internal val winner: PlayerId get() {
        require(isComplete) { "trick is not complete" }

        var winnerSoFar: PlayedCard = _playedCards.first()

        for (playedCard in _playedCards.drop(1)) {
            if (playedCard.card.beats(winnerSoFar.card, context)) {
                winnerSoFar = playedCard
                continue
            }

            if (hasMermaid && hasSkullKing) {
                winnerSoFar = _playedCards.first { it.card == SpecialCard.mermaid }
                break
            }
        }

        return winnerSoFar.playerId
    }

    internal fun add(playedCard: PlayedCard) {
        _playedCards.add(playedCard)

        if (playedCard.card is NumberedCard && suit == null) {
            suit = playedCard.card.suit
        }

        if (playedCard.card is SpecialCard) {
            specialsPlayed.add(playedCard.card.suit)
        }
    }

    internal fun isCardPlayable(card: Card, restOfHand: List<Card>): Boolean {
        if (restOfHand.isEmpty() || suit == null) return true

        when(card) {
            is NumberedCard -> {
                if (card.suit == suit) return true
                return restOfHand.none { it is NumberedCard && it.suit == suit }
            }
            is SpecialCard -> return true
        }
    }

    private val context get (): TrickContext = TrickContext(
        suit = suit,
        hasSkullKing = hasSkullKing,
        hasMermaid = hasMermaid,
        hasPirate = hasPirate
    )
}

private data class TrickContext(
    val suit: Suit?,
    val hasSkullKing: Boolean,
    val hasMermaid: Boolean,
    val hasPirate: Boolean
) {
    fun suitIs(suit: Suit) = this.suit == suit
}

private fun Card.beats(other: Card, context: TrickContext): Boolean {
    return when(this) {
        is NumberedCard -> beats(other, context)
        is SpecialCard -> beats(other, context)
    }
}

private fun NumberedCard.beats(other: Card, context: TrickContext): Boolean {
    when(other) {
        is NumberedCard -> {
            val areEitherBlack = suit == Black || other.suit == Black

            if (areEitherBlack && !context.suitIs(Black)) {
                if (suit == other.suit) return number > other.number
                return suit == Black
            }

            if (context.suitIs(suit)) return number > other.number
            return false
        }
        is SpecialCard -> return when(other.suit) {
            Escape -> true
            Pirate,
            Mermaid,
            SkullKing -> false
        }
    }
}

private fun SpecialCard.beats(other: Card, context: TrickContext): Boolean {
    return when(other) {
        is NumberedCard -> suit != Escape
        is SpecialCard -> {
            if (suit == other.suit) return false

            when(suit) {
                Escape -> false
                Pirate -> !context.hasSkullKing
                Mermaid -> context.hasSkullKing || !context.hasPirate
                SkullKing -> !context.hasMermaid
            }
        }
    }
}
