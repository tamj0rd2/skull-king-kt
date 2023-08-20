package com.tamj0rd2.webapp

import App
import GameEvent
import PlayerId
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

fun wsHandler(app: App): RoutingWsHandler {
    val playerIdPath = Path.of("playerId")
    val gameEventLens = WsMessage.auto<GameEvent>().toLens()

    return websockets(
        "/{playerId}" bind { req: Request ->
            WsResponse { ws: Websocket ->
                val playerId = playerIdPath(req)

                app.subscribeToGameEvents(playerId) {
                    ws.send(gameEventLens(it))
                }

                ws.onMessage {
                    try {
                        when(val message = it.bodyString().asJsonObject().asA(ClientMessage::class)) {
                            is ClientMessage.BetPlaced -> {
                                app.game?.placeBet(message.playerId, message.bet) ?: error("game is null")
                            }
                        }
                    } catch (e: Exception) {
                        println("ERROR!! failed to handle client message: ${it.bodyString()}\n${e.stackTraceToString()}")
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
    }

    data class BetPlaced(val playerId: PlayerId, val bet: Int) : ClientMessage() {
        override val type = Type.BetPlaced
    }
}