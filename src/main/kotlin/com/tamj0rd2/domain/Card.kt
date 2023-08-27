package com.tamj0rd2.domain

data class Card(val id: CardId) {
    override fun toString(): String {
        return "card-$id"
    }

    fun playedBy(playerId: PlayerId): PlayedCard = PlayedCard(playerId, this)
}

typealias CardId = String