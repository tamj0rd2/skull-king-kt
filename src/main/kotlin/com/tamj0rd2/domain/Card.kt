package com.tamj0rd2.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class Card(val name: CardName) {
    override fun toString(): String {
        return name.lowercase(Locale.getDefault())
    }

    fun playedBy(playerId: PlayerId): PlayedCard = PlayedCard(playerId, this)

    companion object {
        fun from(suit: String, number: Int?): Card {
            return number?.let { NumberedCard(Suit.from(suit), it) } ?: SpecialCard(SpecialSuit.from(suit))
        }
    }

    data class NumberedCard(val suit: Suit, val number: Int) : Card("$suit-$number") {
        init {
            require(number in 1..13) { "number must be between 1 and 13" }
        }
    }

    data class SpecialCard(val suit: SpecialSuit) : Card(suit.name) {
        companion object {
            val escape get(): SpecialCard = SpecialCard(SpecialSuit.Escape)
            val pirate get(): SpecialCard = SpecialCard(SpecialSuit.Pirate)
            val mermaid get(): SpecialCard = SpecialCard(SpecialSuit.Mermaid)
            val skullKing get(): SpecialCard = SpecialCard(SpecialSuit.SkullKing)
        }
    }
}

typealias CardName = String

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

val Int.blue: Card get() = Card.NumberedCard(Suit.Blue, this)
val Int.yellow: Card get() = Card.NumberedCard(Suit.Yellow, this)
val Int.red: Card get() = Card.NumberedCard(Suit.Red, this)
val Int.black: Card get() = Card.NumberedCard(Suit.Black, this)


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
