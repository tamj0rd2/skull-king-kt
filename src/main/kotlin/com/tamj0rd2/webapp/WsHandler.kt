package com.tamj0rd2.webapp

import com.tamj0rd2.domain.App
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.PlayerId
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.http4k.core.Request
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asJsonObject
import org.http4k.format.Jackson.auto
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
    val logger = LoggerFactory.getLogger("wsHandler")

    return websockets(
        "/{playerId}" bind { req: Request ->
            WsResponse { ws: Websocket ->
                val playerId = playerIdPath(req)

                app.game.subscribeToGameEvents(playerId) {
                    ws.send(gameEventLens(it))
                }

                ws.onMessage {
                    logger.info("received message: ${it.bodyString()}")

                    when(val message = it.bodyString().asJsonObject().asA(ClientMessage::class)) {
                        is ClientMessage.BetPlaced -> app.game.placeBet(message.playerId, message.bet)
                        is ClientMessage.UnhandledGameEvent -> logger.error("CLIENT ERROR: unhandled game event: ${message.offender}")
                        is ClientMessage.Error -> logger.error("CLIENT ERROR: ${message.stackTrace}")
                        is ClientMessage.CardPlayed -> app.game.playCard(playerId, message.cardId)
                    }
                }
            }
        }
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
// NOTE: for this parsing to work, the FE needs to specifically reference ClientMessage$SubTypeName.
// the prefix wouldn't be necessary if I didn't nest the Subtypes here, but I wanted to for better organisation :D
sealed class ClientMessage {
    abstract val type: Type

    enum class Type {
        BetPlaced,
        UnhandledGameEvent,
        Error,
        CardPlayed,
    }

    // TODO maybe this is a GameEvent, rather than a ClientMessage? Or perhaps, a ClientMessage that happens to contain a GameEvent?
    data class BetPlaced(val playerId: PlayerId, val bet: Int) : ClientMessage() {
        override val type = Type.BetPlaced
    }

    data class CardPlayed(val cardId: CardId) : ClientMessage() {
        override val type = Type.CardPlayed
    }

    data class UnhandledGameEvent(val offender: GameEvent.Type) : ClientMessage() {
        override val type = Type.UnhandledGameEvent
    }

    data class Error(val stackTrace: String) : ClientMessage() {
        override val type = Type.Error
    }
}