package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import org.http4k.lens.Path
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
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

    if (automateGameMasterCommands) {
        AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
    }

    return websockets(
        "/{playerId}" bind { req ->
            val playerId = playerIdLens(req)
            val logger = LoggerFactory.getLogger("$playerId:wsHandler")

            WsResponse { ws ->
                val ack = Acknowledgements()

                game.subscribeToGameEvents { event ->
                    val messages = event.asMessagesToClient(game, playerId)

                    if (messages.isNotEmpty()) {
                        val otwMessage = OverTheWireMessage.MessagesToClient(messages.toList())
                        logger.sending(otwMessage)

                        ws.send(overTheWireMessageLens(otwMessage))
                        logger.awaitingAck(otwMessage)

                        ack.waitFor(otwMessage.messageId)
                    }
                }

                ws.onError { logger.error(it.message, it) }

                ws.onMessage {
                    val otwMessage = overTheWireMessageLens(it)
                    logger.receivedMessage(otwMessage)

                    when (otwMessage) {
                        is OverTheWireMessage.Acknowledgement -> {
                            ack(otwMessage.id)
                            logger.processedMessage(otwMessage)
                        }

                        is OverTheWireMessage.MessageToServer -> {
                            when (val message = otwMessage.message) {
                                is MessageFromClient.BidPlaced -> game.bid(playerId, message.bid)
                                is MessageFromClient.UnhandledServerMessage -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                                is MessageFromClient.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                                is MessageFromClient.CardPlayed -> game.playCard(playerId, message.cardName)
                            }

                            logger.processedMessage(otwMessage)
                            ws.send(overTheWireMessageLens(otwMessage.acknowledge()))
                            logger.sentAckFor(otwMessage)
                        }

                        else -> error("invalid message from client to server: $otwMessage")
                    }
                }

                logger.info("connected")

                val response = MessageToClient.YouJoined(
                    game.players,
                    game.isInState(GameState.WaitingForMorePlayers)
                ).overTheWire()

                logger.sending(response)
                ws.send(overTheWireMessageLens(response))
            }
        }
    )
}

fun GameEvent.asMessagesToClient(game: Game, thisPlayerId: PlayerId): List<MessageToClient> {
    fun ifNotActor(
        playerId: PlayerId,
        default: List<MessageToClient> = emptyList(),
        block: () -> List<MessageToClient>
    ) = if (playerId == thisPlayerId) default else block()

    return when (this) {
        is GameEvent.BidPlaced -> ifNotActor(playerId) { listOf(MessageToClient.BidPlaced(playerId)) }
        is GameEvent.BiddingCompleted -> listOf(MessageToClient.BiddingCompleted(bids))
        is GameEvent.CardPlayed -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.CardPlayed(
                    playerId = playerId,
                    card = card,
                    nextPlayer = game.currentPlayersTurn
                ),
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(MessageToClient.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            messages
        }

        is GameEvent.CardsDealt -> TODO("add cards dealt event")
        is GameEvent.GameCompleted -> listOf(MessageToClient.GameCompleted)
        is GameEvent.GameStarted -> listOf(MessageToClient.GameStarted(players))
        is GameEvent.PlayerJoined -> listOf(
            MessageToClient.PlayerJoined(
                playerId,
                game.isInState(GameState.WaitingForMorePlayers)
            )
        )

        is GameEvent.RoundStarted -> listOf(
            MessageToClient.RoundStarted(
                game.getCardsInHand(thisPlayerId),
                roundNumber
            )
        )

        is GameEvent.TrickCompleted -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.TrickCompleted(winner)
            )

            if (game.trickNumber == game.roundNumber) {
                messages += MessageToClient.RoundCompleted(game.winsOfTheRound)
            }

            messages
        }

        is GameEvent.TrickStarted -> {
            val messages = mutableListOf<MessageToClient>(
                MessageToClient.TrickStarted(
                    trickNumber,
                    game.currentPlayersTurn ?: error("currentPlayer is null")
                )
            )

            if (game.currentPlayersTurn == thisPlayerId) {
                messages.add(MessageToClient.YourTurn(game.getCardsInHand(thisPlayerId)))
            }

            messages
        }
    }
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
