package com.tamj0rd2.webapp

import com.tamj0rd2.domain.App
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.GameEvent
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

fun wsHandler(app: App): RoutingWsHandler {
    val playerIdPath = Path.of("playerId")
    val gameEventLens = WsMessage.auto<GameEvent>().toLens()
    val clientMessageLens = WsMessage.auto<ClientMessage>().toLens()

    return websockets(
        "/{playerId}" bind { req: Request ->
            WsResponse { ws: Websocket ->
                val playerId = playerIdPath(req)
                val logger = LoggerFactory.getLogger("wsHandler pid='$playerId'")

                app.game.subscribeToGameEvents(playerId) {
                    logger.info("sending game event to $playerId: ${it.asJsonObject()}")
                    ws.send(gameEventLens(it))
                }

                ws.onMessage {
                    logger.info("received client message from $playerId: ${it.bodyString()}")

                    when(val message = clientMessageLens(it)) {
                        is ClientMessage.BidPlaced -> app.game.bid(playerId, message.bid)
                        is ClientMessage.UnhandledMessageFromServer -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                        is ClientMessage.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                        is ClientMessage.CardPlayed -> app.game.playCard(playerId, message.cardName)
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

