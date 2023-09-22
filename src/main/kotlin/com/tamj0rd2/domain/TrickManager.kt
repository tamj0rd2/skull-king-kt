package com.tamj0rd2.domain

class TrickManager(private val size: Int) {

    private val _cards = mutableListOf<PlayedCard>()
    val cards: List<PlayedCard> get() = _cards
    val isComplete: Boolean get() = _cards.size == size

    fun add(playedCard: PlayedCard) {
        _cards += playedCard
    }

    fun clear() {
        _cards.clear()
    }
}
