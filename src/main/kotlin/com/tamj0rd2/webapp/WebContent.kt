package com.tamj0rd2.webapp

import App
import PlayerId
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun main() {
    // if setting this to true, remember to run the app with the working directory set to the root of the example
    val hotReload = true

    val httpHandler = webContent(Clock.systemDefaultZone(), hotReload, App())
    PolyHandler(httpHandler).asServer(Jetty(8080)).start()
}

private data class Home(val time: String, val browser: String) : ViewModel
private data class Game(val players: List<PlayerId>, val waitingForMorePlayers: Boolean) : ViewModel

fun webContent(clock: Clock, hotReload: Boolean, app: App): HttpHandler {
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)

    return routes(
        static(resourceLoader),
        "/" bind Method.GET to {
            val view = Body.viewModel(renderer, TEXT_HTML).toLens()

            val model = Home(LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME), it.header("User-Agent") ?: "unknown")

            Response(OK).with(view of model)
        },

        "/" bind Method.POST to {
            val playerId = it.form("playerId") ?: error("playerId not posted!")
            app.addPlayerToRoom(playerId)

            val view = Body.viewModel(renderer, TEXT_HTML).toLens()
            val model = Game(players = app.players, waitingForMorePlayers = app.waitingForMorePlayers)
            Response(OK).with(view of model)
        }
    )
}

private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}
