package com.tamj0rd2.webapp

import App
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

// TODO: I almost definitely shouldn't reuse this Game viewmodel... it's just for loading the initial page
data class GameEventMessage(val gameEvent: GameEvent, val gameState: GameState)

fun wsHandler(app: App): RoutingWsHandler {
    val playerIdPath = Path.of("playerId")
    val gameEventMessageLens = WsMessage.auto<GameEventMessage>().toLens()

    return websockets(
        "/{playerId}" bind { req: Request ->
            WsResponse { ws: Websocket ->
                val name = playerIdPath(req)

                println("there was a ws connection for $name")
                app.subscribeToGameEvents {
                    println(it)
                    ws.send(gameEventMessageLens(GameEventMessage(it, app.toGameState(8080, name))))
                }

                ws.send(WsMessage("hello $name"))
                ws.onMessage {
                    ws.send(WsMessage("$name is responding"))
                }
                ws.onClose { println("$name is closing") }
            }
        }
    )
}

data class GameState(
    val waitingForMorePlayers: Boolean
)

private fun App.toGameState(port: Int, playerId: PlayerId) = GameState(
    waitingForMorePlayers = waitingForMorePlayers,
)
