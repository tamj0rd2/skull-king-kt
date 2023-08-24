package com.tamj0rd2.webapp

import com.tamj0rd2.domain.App
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hands
import com.tamj0rd2.domain.PlayerId
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asJsonObject
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

private data class Game(
    val wsHost: String,
    val players: List<PlayerId>,
    val waitingForMorePlayers: Boolean,
    val playerId: PlayerId,
) : ViewModel

fun httpHandler(port: Int, hotReload: Boolean, app: App): HttpHandler {
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)

    return routes(
        static(resourceLoader),
        "/" bind Method.GET to {
            val body = resourceLoader.load("index.html")?.readText() ?: error("index.html not found!")
            Response(Status.OK).body(body)
        },
        "/play" bind Method.POST to {
            val playerId = it.form("playerId") ?: error("playerId not posted!")
            app.game.addPlayer(playerId)

            val view = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
            val model = Game(
                wsHost = "ws://localhost:$port",
                players = app.game.players,
                waitingForMorePlayers = app.game.state == GameState.WaitingForMorePlayers,
                playerId = playerId,
            )
            Response(Status.OK).with(view of model)
        },
        "/startGame" bind Method.POST to {
            app.game.start()
            Response(Status.OK)
        },
        "/rigDeck" bind Method.PUT to { req ->
            val command = req.bodyString().asJsonObject().asA(RigDeckCommand::class)
            app.game.rigDeck(command.hands)
            Response(Status.OK)
        }
    )
}

data class RigDeckCommand(val hands: Hands)

private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}