package com.tamj0rd2.webapp

import App
import PlayerId
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class Home(val time: String, val browser: String) : ViewModel
data class Game(
    val wsHost: String,
    val players: List<PlayerId>,
    val waitingForMorePlayers: Boolean,
    val playerId: PlayerId,
) : ViewModel

fun httpHandler(port: Int, clock: Clock, hotReload: Boolean, app: App): HttpHandler {
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)

    return routes(
        static(resourceLoader),
        "/" bind Method.GET to {
            val view = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

            val model = Home(
                LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                it.header("User-Agent") ?: "unknown"
            )

            Response(Status.OK).with(view of model)
        },

        "/" bind Method.POST to {
            val playerId = it.form("playerId") ?: error("playerId not posted!")
            app.addPlayerToRoom(playerId)

            val view = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
            val model = Game(
                wsHost = "ws://localhost:$port",
                players = app.players,
                waitingForMorePlayers = app.waitingForMorePlayers,
                playerId = playerId,
            )
            Response(Status.OK).with(view of model)
        }
    )
}

private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}