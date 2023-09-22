package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer
import kotlin.time.Duration

object Server {
    @JvmStatic fun main(args: Array<String>) {
        make(
            port = 8080,
            hotReload = true,
            automateGameMasterCommands = true
        ).start()
    }

    fun make(
        port: Int,
        hotReload: Boolean = false,
        automateGameMasterCommands: Boolean = false,
        automaticGameMasterDelayOverride: Duration? = null
    ): Http4kServer {
        val game = Game()
        val http = httpHandler(game, port, hotReload, automateGameMasterCommands)
        val ws = wsHandler(game, automateGameMasterCommands, automaticGameMasterDelayOverride)
        return PolyHandler(http, ws).asServer(Undertow(port))
    }
}
