package com.tamj0rd2.domain

import com.tamj0rd2.domain.Card.SpecialCard

data class Deck(private val cards: MutableList<Card>) {
    companion object {
        fun new(): Deck {
            val numberedCards = (1..13).flatMap { listOf(it.red, it.yellow, it.blue, it.black) }
            val specialCards = listOf(
                *SpecialCard.escape.repeat(5),
                *SpecialCard.pirate.repeat(5),
                *SpecialCard.mermaid.repeat(2),
                SpecialCard.skullKing,
                // TODO: add scary mary
                // SpecialCard.scaryMary,
            )

            return Deck((numberedCards + specialCards).shuffled().toMutableList())
        }
    }

    fun takeCards(count: Int): List<Card> = (1..count).map { cards.removeFirst() }
}

private fun Card.repeat(i: Int): Array<out Card> {
    return Array(i) { this }
}
