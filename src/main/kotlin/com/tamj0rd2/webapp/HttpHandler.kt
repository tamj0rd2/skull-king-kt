package com.tamj0rd2.webapp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tamj0rd2.domain.App
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import org.slf4j.LoggerFactory

private data class Game(
    val wsHost: String,
    val players: List<PlayerId>,
    val waitingForMorePlayers: Boolean,
    val playerId: PlayerId,
) : ViewModel

data class Index(val errorMessage: String? = null) : ViewModel {
    companion object {
        val withoutError = Index()
        fun withError(errorMessage: String) = Index(errorMessage)
    }
}

fun httpHandler(port: Int, hotReload: Boolean, app: App): HttpHandler {
    val logger = LoggerFactory.getLogger("httpHandler")
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)
    val gameMasterCommandLens = Body.auto<GameMasterCommand>().toLens()

    val indexView = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
    return routes(
        static(resourceLoader),
        "/" bind Method.GET to {
            Response(Status.OK).with(indexView of Index.withoutError)
        },
        "/play" bind Method.POST to {
            val playerId = it.form("playerId") ?: error("playerId not posted!")

            try {
                app.game.addPlayer(playerId)
            } catch (e: GameException.PlayerWithSameNameAlreadyJoined) {
                return@to Response(Status.OK).with(indexView of Index.withError(e::class.simpleName!!))
            }

            val view = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
            val model = Game(
                wsHost = "ws://localhost:$port",
                players = app.game.players,
                waitingForMorePlayers = app.game.state == GameState.WaitingForMorePlayers,
                playerId = playerId,
            )
            Response(Status.OK).with(view of model)
        },
        "/startGame" bind Method.POST to { app.game.start().respondOK() },
        "/do-game-master-command" bind Method.POST to { req ->
            logger.info("received command: ${req.bodyString()}")

            when (val command = gameMasterCommandLens(req)) {
                is GameMasterCommand.StartGame -> app.game.start().respondOK()
                is GameMasterCommand.RigDeck -> app.game.rigDeck(command.playerId, command.cards).respondOK()
                is GameMasterCommand.StartNextRound -> app.game.startNextRound().respondOK()
                is GameMasterCommand.StartNextTrick -> app.game.startNextTrick().respondOK()
            }
        }
    )
}

private fun Any.respondOK() = Response(Status.OK)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class GameMasterCommand {
    object StartGame : GameMasterCommand()
    data class RigDeck(val playerId: PlayerId, val cards: List<Card>) : GameMasterCommand()
    object StartNextRound : GameMasterCommand()
    object StartNextTrick : GameMasterCommand()
}


private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}