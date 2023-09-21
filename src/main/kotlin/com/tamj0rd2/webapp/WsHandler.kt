package com.tamj0rd2.webapp

import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.core.Request
import org.http4k.lens.Path
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.slf4j.LoggerFactory

internal fun wsHandler(game: Game, gameEventHandler: GameEventHandler): RoutingWsHandler {
    val playerIdPath = Path.of("playerId")
    val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
    val clientMessageLens = WsMessage.auto<ClientMessage>().toLens()

    return websockets(
        "/{playerId}" bind { req: Request ->
            WsResponse { ws: Websocket ->
                val playerId: PlayerId = playerIdPath(req)
                val logger = LoggerFactory.getLogger("wsHandler pid='$playerId'")

                gameEventHandler.subscribeToGameEvents(playerId) {
                    logger.info("sending game event to $playerId: ${it.asJsonObject()}")
                    val messageToClient: MessageToClient = when(it) {
                        is GameEvent.BidPlaced -> MessageToClient.BidPlaced(it.playerId)
                        is GameEvent.BiddingCompleted -> MessageToClient.BiddingCompleted(it.bids)
                        is GameEvent.CardPlayed -> MessageToClient.CardPlayed(it.playerId, it.card, game.currentPlayersTurn)
                        is GameEvent.CardsDealt -> TODO("add cards dealt event")
                        is GameEvent.GameCompleted -> MessageToClient.GameCompleted
                        is GameEvent.GameStarted -> MessageToClient.GameStarted(it.players)
                        is GameEvent.PlayerJoined -> MessageToClient.PlayerJoined(it.playerId, game.isInState(GameState.WaitingForMorePlayers))
                        is GameEvent.RoundStarted -> MessageToClient.RoundStarted(game.getCardsInHand(playerId), it.roundNumber)
                        is GameEvent.TrickCompleted -> MessageToClient.TrickCompleted
                        is GameEvent.TrickStarted -> MessageToClient.TrickStarted(it.trickNumber, game.currentPlayersTurn ?: error("currentPlayer is null"))
                    }

                    ws.send(messageToClientLens(messageToClient))
                }

                ws.onMessage {
                    logger.info("received client message from $playerId: ${it.bodyString()}")

                    when(val message = clientMessageLens(it)) {
                        is ClientMessage.BidPlaced -> game.bid(playerId, message.bid)
                        is ClientMessage.UnhandledMessageFromServer -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                        is ClientMessage.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                        is ClientMessage.CardPlayed -> game.playCard(playerId, message.cardName)
                    }
                }
            }
        }
    )
}

// For this parsing to work, the FE needs to specifically reference ClientMessage$SubTypeName.
// the prefix wouldn't be necessary if I didn't nest the Subtypes here, but I wanted better organisation :D
sealed class ClientMessage {
    data class BidPlaced(val bid: Int) : ClientMessage()

    data class CardPlayed(val cardName: CardName) : ClientMessage()

    data class UnhandledMessageFromServer(val offender: String) : ClientMessage()

    data class Error(val stackTrace: String) : ClientMessage()
}

