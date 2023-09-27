package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameEventListener
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.lens.Path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.slf4j.LoggerFactory
import kotlin.time.Duration

class GameWsHandler(
    private val game: Game,
    automateGameMasterCommands: Boolean,
    automaticGameMasterDelayOverride: Duration?
) {
    init {
        if (automateGameMasterCommands) {
            AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
        }
    }

    private val playerIdLens = Path.map(::PlayerId, PlayerId::playerId).of("playerId")
    private val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
    private val clientMessageLens = WsMessage.auto<MessageFromClient>().toLens()

    val handler = websockets(
        "/{playerId}" bind { req ->
            WsResponse { ws ->
                val playerId = playerIdLens(req)
                val logger = LoggerFactory.getLogger("wsHandler:$playerId")
                logger.info("$playerId is trying to join")

                if (game.players.contains(playerId)) {
                    logger.error("player with id '$playerId' already joined")
                    ws.close(WsStatus.REFUSE)
                    return@WsResponse
                }

                var hasConnected = false
                val heldMessages = mutableListOf<MessageToClient>()
                val lock = Any()

                game.subscribeToGameEvents(listenerFor(playerId) { message ->
                    if (!hasConnected) {
                        heldMessages.add(message)
                        return@listenerFor
                    }

                    logger.info("sending message to $playerId: ${message.asJsonObject()}")
                    ws.send(messageToClientLens(message))
                })

                ws.onMessage {
                    synchronized(lock) {
                        logger.info("server received: ${it.bodyString()}")

                        when (val message = clientMessageLens(it)) {
                            is MessageFromClient.BidPlaced -> game.bid(playerId, message.bid)
                            is MessageFromClient.UnhandledServerMessage -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                            is MessageFromClient.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                            is MessageFromClient.CardPlayed -> game.playCard(playerId, message.cardName)
                            is MessageFromClient.Connected -> {
                                if (!hasConnected) {
                                    hasConnected = true
                                    heldMessages.forEach { heldMessage ->
                                        logger.info("sending held message to $playerId: ${heldMessage.asJsonObject()}")
                                        ws.send(messageToClientLens(heldMessage))
                                    }
                                }
                            }
                        }
                    }
                }

                game.addPlayer(playerId)
            }
        }
    )

    private fun listenerFor(
        playerId: PlayerId,
        sendMessage: (MessageToClient) -> Unit
    ) = GameEventListener { event ->
        sendMessage(when (event) {
            is GameEvent.BidPlaced -> MessageToClient.BidPlaced(event.playerId)
            is GameEvent.BiddingCompleted -> MessageToClient.BiddingCompleted(event.bids)
            is GameEvent.CardPlayed -> {
                val messages = mutableListOf<MessageToClient>(
                    MessageToClient.CardPlayed(
                        playerId = event.playerId,
                        card = event.card,
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
            is GameEvent.GameStarted -> MessageToClient.GameStarted(event.players)
            is GameEvent.PlayerJoined -> MessageToClient.PlayerJoined(
                event.playerId,
                game.players,
                game.isInState(GameState.WaitingForMorePlayers)
            )

            is GameEvent.RoundStarted -> MessageToClient.RoundStarted(
                game.getCardsInHand(playerId),
                event.roundNumber
            )


            is GameEvent.TrickCompleted -> {
                val messages = mutableListOf<MessageToClient>(
                    MessageToClient.TrickCompleted(event.winner)
                )

                if (game.trickNumber == game.roundNumber) {
                    messages += MessageToClient.RoundCompleted(game.winsOfTheRound)
                }

                MessageToClient.Multi(messages)
            }

            is GameEvent.TrickStarted -> {
                val messages = mutableListOf<MessageToClient>(
                    MessageToClient.TrickStarted(
                        event.trickNumber,
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
        })
    }
}
