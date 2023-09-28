package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameEventListener
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.lens.Path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.slf4j.LoggerFactory
import java.time.Instant
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
            val syncObject = Object()
            val messageAcknowledgements = mutableMapOf<MessageId, Boolean>()

            WsResponse { ws ->
                val playerId = playerIdLens(req)
                val logger = LoggerFactory.getLogger("wsHandler:$playerId")
                ws.onError { logger.error("websocket error", it) }
                logger.info("$playerId is connecting")

                val sendMessage = { message: MessageToClient ->
                    logger.info("sending: $message")
                    ws.send(messageToClientLens(message))

                    if (message.needsAck) {
                        messageAcknowledgements[message.id] = false
                        synchronized(syncObject) {
                            logger.info("waiting for ack of '${message::class.simpleName}'")
                            val mustFinishBy = Instant.now().plusMillis(3000)

                            do {
                                if (messageAcknowledgements[message.id] == true) {
                                    logger.info("got ack of '${message::class.simpleName}'")
                                    return@synchronized
                                }
                                syncObject.wait(50)
                            } while (Instant.now() < mustFinishBy)

                            error("message '${message::class.simpleName}' not acked by $playerId")
                        }
                    }
                }

                if (game.players.contains(playerId)) {
                    logger.error("player with id '$playerId' already joined")
                    ws.close(WsStatus.REFUSE)
                    return@WsResponse
                }

                game.subscribeToGameEvents(listenerFor(playerId, sendMessage))

                ws.onMessage {
                    synchronized(syncObject) {
                        val message = clientMessageLens(it)
                        logger.info("received: $message")

                        when (message) {
                            is MessageFromClient.BidPlaced -> game.bid(playerId, message.bid)
                            is MessageFromClient.UnhandledServerMessage -> error("CLIENT ERROR: unhandled game event: ${message.offender}")
                            is MessageFromClient.Error -> error("CLIENT ERROR: ${message.stackTrace}")
                            is MessageFromClient.CardPlayed -> game.playCard(playerId, message.cardName)
                            is MessageFromClient.JoinGame -> game.addPlayer(playerId)
                            is MessageFromClient.Ack -> {
                                val messageId = message.acked.id
                                require(messageAcknowledgements.contains(messageId)) { "message $messageId doesn't exist in acknowledgements map" }
                                require(messageAcknowledgements[messageId] == false) { "message $messageId has already been acknowledged" }

                                messageAcknowledgements[messageId] = true
                                syncObject.notify()
                                return@onMessage
                            }
                        }

                        if (message.needsAck) sendMessage(message.ack())
                    }
                }
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
                MessageToClient.multi(listOfNotNull(
                    MessageToClient.CardPlayed(
                        playerId = event.playerId,
                        card = event.card,
                        nextPlayer = game.currentPlayersTurn
                    ),
                    ifTrue(game.currentPlayersTurn == playerId) {
                        // TODO: make a new method called: "cardsWithPlayability" or something
                        val playableCards = game.getCardsInHand(playerId)
                            .map { it.name to game.isCardPlayable(playerId, it) }
                            .toMap()
                        MessageToClient.YourTurn(playableCards)
                    },
                ))
            }

            is GameEvent.CardsDealt -> TODO("add cards dealt event")
            is GameEvent.GameCompleted -> MessageToClient.GameCompleted()
            is GameEvent.GameStarted -> MessageToClient.GameStarted(event.players)
            is GameEvent.PlayerJoined -> MessageToClient.PlayerJoined(
                playerId = event.playerId,
                players = game.players,
                waitingForMorePlayers = game.isInState(GameState.WaitingForMorePlayers),
                needsAck = event.playerId != playerId,
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
                MessageToClient.multi(listOfNotNull(
                    MessageToClient.TrickStarted(
                        event.trickNumber,
                        game.currentPlayersTurn ?: error("currentPlayer is null")
                    ),
                    ifTrue(game.currentPlayersTurn == playerId) {
                        // TODO: make a new method called: "cardsWithPlayability" or something
                        val playableCards = game.getCardsInHand(playerId)
                            .map { it.name to game.isCardPlayable(playerId, it) }
                            .toMap()
                        MessageToClient.YourTurn(playableCards)
                    },
                ))
            }
        })
    }
}

fun <T> ifTrue(condition: Boolean, block: () -> T): T? {
    return if (condition) block() else null
}
