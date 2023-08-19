package com.tamj0rd2.webapp

import App
import Card
import GameEvent
import PlayerId
import org.http4k.core.Request
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
                    println(">CLIENT $playerId: ${it.bodyString()}")
                }
            }
        }
    )
}
