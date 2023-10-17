package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.lens.Path
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.timerTask
import kotlin.time.Duration

internal fun wsHandler(
    game: Game,
    automateGameMasterCommands: Boolean,
    automaticGameMasterDelayOverride: Duration?
): RoutingWsHandler {
    val playerIdLens = Path.map(::PlayerId, PlayerId::playerId).of("playerId")
    val clientMessageLens = WsMessage.auto<MessageFromClient>().toLens()

    if (automateGameMasterCommands) {
        AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
    }

    return websockets(
        "/{playerId}" bind { req ->
            val playerId = playerIdLens(req)
            val logger = LoggerFactory.getLogger("$playerId:wsHandler")

            WsResponse { ws ->
                game.subscribeToGameEvents {
                    val messageToClient: MessageToClient = when (it) {
                        is GameEvent.BidPlaced -> MessageToClient.BidPlaced(it.playerId)
                        is GameEvent.BiddingCompleted -> MessageToClient.BiddingCompleted(it.bids)
                        is GameEvent.CardPlayed -> {
                            val messages = mutableListOf<MessageToClient>(
                                MessageToClient.CardPlayed(
                                    playerId = it.playerId,
                                    card = it.card,
                                    nextPlayer = game.currentPlayersTurn
                                ),
                            )

                            if (game.currentPlayersTurn == playerId) {
                                // TODO: make a new method called: "cardsWithPlayability" or something
                                val cardsWithPlayability = game.getCardsInHand(playerId)
                                    .map { it.name to game.isCardPlayable(playerId, it) }
                                    .toMap()
                                messages.add(MessageToClient.YourTurn(cardsWithPlayability))
                            }

                            MessageToClient.Multi(messages)
                        }

                        is GameEvent.CardsDealt -> TODO("add cards dealt event")
                        is GameEvent.GameCompleted -> MessageToClient.GameCompleted
                        is GameEvent.GameStarted -> MessageToClient.GameStarted(it.players)
                        is GameEvent.PlayerJoined -> MessageToClient.PlayerJoined(
                                it.playerId,
                                game.isInState(GameState.WaitingForMorePlayers)
                            )

                        is GameEvent.RoundStarted -> MessageToClient.RoundStarted(
                                game.getCardsInHand(playerId),
                                it.roundNumber
                            )


                        is GameEvent.TrickCompleted -> {
                            val messages = mutableListOf<MessageToClient>(
                                MessageToClient.TrickCompleted(it.winner)
                            )

                            if (game.trickNumber == game.roundNumber) {
                                messages += MessageToClient.RoundCompleted(game.winsOfTheRound)
                            }

                            MessageToClient.Multi(messages)
                        }
                        is GameEvent.TrickStarted -> {
                            val messages = mutableListOf<MessageToClient>(
                                MessageToClient.TrickStarted(
                                    it.trickNumber,
                                    game.currentPlayersTurn ?: error("currentPlayer is null")
                                )
                            )

                            if (game.currentPlayersTurn == playerId) {
                                // TODO: make a new method called: "cardsWithPlayability" or something
                                val cardsWithPlayability = game.getCardsInHand(playerId)
                                    .map { it.name to game.isCardPlayable(playerId, it) }
                                    .toMap()
                                messages.add(MessageToClient.YourTurn(cardsWithPlayability))
                            }

                            MessageToClient.Multi(messages)
                        }
                    }

                    logger.info("sending message to $playerId: $messageToClient")
                    ws.send(messageToClientLens(messageToClient))
                }

                ws.onMessage {
                    logger.info("received client message from $playerId: ${it.bodyString()}")

                    when (val message = clientMessageLens(it)) {
                        is MessageFromClient.BidPlaced -> game.bid(playerId, message.bid)
                        is MessageFromClient.UnhandledServerMessage -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                        is MessageFromClient.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                        is MessageFromClient.CardPlayed -> game.playCard(playerId, message.cardName)
                    }
                }

                logger.info("connected")
                ws.send(messageToClientLens(MessageToClient.YouJoined(game.players, game.isInState(GameState.WaitingForMorePlayers))))
            }
        }
    )
}

internal sealed class MessageFromClient {
    data class BidPlaced(val bid: Int) : MessageFromClient()

    data class CardPlayed(val cardName: CardName) : MessageFromClient()

    data class UnhandledServerMessage(val offender: String) : MessageFromClient()

    data class Error(val stackTrace: String) : MessageFromClient()
}


private class AutomatedGameMaster(private val game: Game, private val delayOverride: Duration?) {
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
