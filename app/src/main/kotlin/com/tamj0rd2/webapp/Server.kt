package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.webapp.Frontend.*
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.asServer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val host = System.getenv("HOST") ?: "localhost"
    val useDevServer = System.getenv("DEV") == "true"

    val frontend = when {
        System.getenv("SVELTE") == "true" -> Svelte
        System.getenv("SOLID") == "true" -> Solid
        else -> WebComponents
    }

    if (useDevServer && frontend.usesViteInDevMode) {
        println("If you're not seeing any content or have console errors, ensure you're running the ${frontend.name} dev server")
    }

    Server.make(
        port = port,
        host = host,
        devServer = useDevServer,
        automateGameMasterCommands = true,
        acknowledgementTimeoutMs = 3000,
        frontend = frontend,
    ).start().apply { println("Server started on port $host:$port") }
}


object Server {
    fun make(
        port: Int,
        host: String = "localhost",
        devServer: Boolean = false,
        automateGameMasterCommands: Boolean = false,
        automaticGameMasterDelayOverride: Duration? = null,
        acknowledgementTimeoutMs: Long = 300,
        gracefulShutdownTimeout: Duration = 1.seconds,
        frontend: Frontend = WebComponents,
    ): Http4kServer {
        val game = Game()

        if (automateGameMasterCommands) {
            AutomatedGameMaster(game, automaticGameMasterDelayOverride).start()
        }

        val httpHandler = HttpHandler(
            game = game,
            host = host,
            devServer = devServer,
            automateGameMasterCommands = automateGameMasterCommands,
            ackTimeoutMs = acknowledgementTimeoutMs,
            frontend = frontend,
        )

        val wsHandler: WsHandler = {
            WsResponse(
                consumer = { ws ->
                    PerPlayerWsHandler(
                        ws = ws,
                        game = game,
                        acknowledgementTimeoutMs = acknowledgementTimeoutMs
                    )
                }
            )
        }

        return PolyHandler(httpHandler, wsHandler).asServer(
            config = Jetty(
                port = port,
                stopMode = StopMode.Graceful(gracefulShutdownTimeout.toJavaDuration())
            )
        )
    }
}
