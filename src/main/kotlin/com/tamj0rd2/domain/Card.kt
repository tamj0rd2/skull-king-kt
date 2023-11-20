package com.tamj0rd2.domain

import com.tamj0rd2.domain.Suit.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
sealed class Card(val name: CardName) {

    fun playedBy(playerId: PlayerId): PlayedCard = PlayedCard(playerId, this)

    companion object {
        fun from(suit: String, number: Int?): Card {
            if (number != null) return NumberedCard(Suit.from(suit), number)
            return SpecialCard(SpecialSuit.from(suit))
        }
    }

    @Serializable
    data class NumberedCard(val suit: Suit, val number: Int) : Card("$suit-$number") {
        init {
            require(number in 1..13) { "number must be between 1 and 13" }
        }

        override fun toString(): String {
            return name.lowercase(Locale.getDefault())
        }
    }

    @Serializable
    data class SpecialCard(val suit: SpecialSuit) : Card(suit.name) {
        override fun toString(): String {
            return name.lowercase(Locale.getDefault())
        }

        companion object {
            val escape get(): SpecialCard = SpecialCard(SpecialSuit.Escape)
            val pirate get(): SpecialCard = SpecialCard(SpecialSuit.Pirate)
            val mermaid get(): SpecialCard = SpecialCard(SpecialSuit.Mermaid)
            val skullKing get(): SpecialCard = SpecialCard(SpecialSuit.SkullKing)
        }
    }
}

// TODO: make this a tiny type
typealias CardName = String

@Serializable
enum class Suit() {
    Red,
    Yellow,
    Blue,
    Black;

    companion object {
        private val mapper = values().associateBy { it.name }

        fun from(suit: String): Suit {
            return mapper[suit] ?: error("unknown suit: $suit")
        }
    }
}

val Int.blue get() = Card.NumberedCard(Blue, this)
val Int.yellow get() = Card.NumberedCard(Yellow, this)
val Int.red get() = Card.NumberedCard(Red, this)
val Int.black get() = Card.NumberedCard(Black, this)

@Serializable
data class CardWithPlayability(val card: Card, val isPlayable: Boolean)

@Serializable
enum class SpecialSuit() {
    Escape,
    Pirate,
    Mermaid,
    SkullKing;
    // TODO: add scary mary
    //ScaryMary;

    companion object {
        private val mapper = values().associateBy { it.name }

        fun from(suit: String): SpecialSuit {
            return mapper[suit] ?: error("unknown special suit: $suit")
        }
    }
}
