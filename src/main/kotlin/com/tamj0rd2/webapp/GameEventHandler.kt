package com.tamj0rd2.webapp

import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameEventListener
import com.tamj0rd2.domain.PlayerId

internal class GameEventHandler : GameEventListener {
    private val gameEventSubscribers = mutableMapOf<PlayerId, (event: GameEvent) -> Unit>()

    override fun handle(event: GameEvent) {
        gameEventSubscribers.values.forEach { subscriber ->
            subscriber(event)
        }
    }

    fun subscribeToGameEvents(playerId: PlayerId, listener: (event: GameEvent) -> Unit) {
        gameEventSubscribers[playerId] = listener
    }
}