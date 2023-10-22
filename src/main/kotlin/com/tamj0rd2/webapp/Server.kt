package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.asServer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object Server {
    fun make(
        port: Int,
        host: String = "localhost",
        hotReload: Boolean = false,
        automateGameMasterCommands: Boolean = false,
        automaticGameMasterDelayOverride: Duration? = null,
        acknowledgementTimeoutMs: Long = 300,
        gracefulShutdownTimeout: Duration = 1.seconds,
    ): Http4kServer {
        val game = Game()
        val http = httpHandler(
            game = game,
            host = host,
            hotReload = hotReload,
            automateGameMasterCommands = automateGameMasterCommands,
        )

        val ws = wsHandler(
            game = game,
            automateGameMasterCommands = automateGameMasterCommands,
            automaticGameMasterDelayOverride = automaticGameMasterDelayOverride,
            acknowledgementTimeoutMs = acknowledgementTimeoutMs
        )

        return PolyHandler(http, ws).asServer(Jetty(port, StopMode.Graceful(gracefulShutdownTimeout.toJavaDuration())))
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
        acknowledgementTimeoutMs = 3000,
    ).start().apply { println("Server started on port $host:$port") }
}
