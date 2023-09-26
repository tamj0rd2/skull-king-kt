package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.timerTask
import kotlin.time.Duration

internal class AutomatedGameMaster(private val game: Game, private val delayOverride: Duration?) {
    private val allGameEvents = CopyOnWriteArrayList<GameEvent>()
    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    fun start() {
        val timer = Timer()

        game.subscribeToGameEvents { event ->
            allGameEvents.add(event)

            when (event) {
                is GameEvent.PlayerJoined -> {
                    if (game.state != GameState.WaitingToStart) return@subscribeToGameEvents
                    timer.schedule(timerTask {
                        if (allGameEvents.last() != event) return@timerTask

                        logger.info("Starting the game")
                        game.start()
                    }, delayOverride?.inWholeMilliseconds ?: 5000)
                }

                is GameEvent.BiddingCompleted -> {
                    timer.schedule(timerTask {
                        val lastEvent = allGameEvents.last()
                        require(lastEvent == event) { "last event was not bidding completed, it was $lastEvent" }

                        logger.info("Starting the first trick")
                        game.startNextTrick()
                    }, delayOverride?.inWholeMilliseconds ?: 3000)
                }

                is GameEvent.TrickCompleted -> {
                    timer.schedule(timerTask {
                        val lastEvent = allGameEvents.last()
                        require(lastEvent == event) { "last event was not trick completed, it was $lastEvent" }

                        // TODO: need to write a test for what happens after round 10 trick 10
                        if (game.roundNumber == game.trickNumber) {
                            logger.info("Starting the next round")
                            game.startNextRound()
                        } else {
                            logger.info("Starting the next trick")
                            game.startNextTrick()
                        }
                    }, delayOverride?.inWholeMilliseconds ?: 3000)
                }

                else -> {}
            }
        }
    }
}