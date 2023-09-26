package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Server {
    fun make(
        port: Int,
        host: String = "localhost",
        hotReload: Boolean = false,
        automateGameMasterCommands: Boolean = false,
        automaticGameMasterDelayOverride: Duration? = null
    ): Http4kServer {
        val game = Game()
        val http = httpHandler(
            game = game,
            host = host,
            hotReload = hotReload,
            automateGameMasterCommands = automateGameMasterCommands,
        )

        val ws = GameWsHandler(game, automateGameMasterCommands, automaticGameMasterDelayOverride).handler
        return PolyHandler(http, ws).asServer(Undertow(port))
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val host = System.getenv("HOST") ?: "localhost"
    Server.make(
        port = port,
        host = host,
        hotReload = true,
        automateGameMasterCommands = true,
        automaticGameMasterDelayOverride = 10.seconds,
    ).start().apply { println("Server started on port $host:$port") }
}
