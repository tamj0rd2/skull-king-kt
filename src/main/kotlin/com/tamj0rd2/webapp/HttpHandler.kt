package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import com.tamj0rd2.webapp.CustomJackson.auto
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
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
import org.slf4j.LoggerFactory

private data class Game(
    val wsHost: String,
    private val players: List<PlayerId>,
    val waitingForMorePlayers: Boolean,
    val playerId: PlayerId,
) : ViewModel {
    // used by the UI
    val playersJson = players.asJsonObject().asCompactJsonString()
}

private data class Index(val errorMessage: String? = null) : ViewModel {
    companion object {
        val withoutError = Index()
        fun withError(errorMessage: String) = Index(errorMessage)
    }
}

internal fun httpHandler(
    game: com.tamj0rd2.domain.Game,
    port: Int,
    hotReload: Boolean,
    automateGameMasterCommands: Boolean
): HttpHandler {
    val logger = LoggerFactory.getLogger("httpHandler")
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)
    val gameMasterCommandLens = Body.auto<GameMasterCommand>().toLens()

    val wsHost = "ws://localhost:$port"
    val htmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

    fun gameMasterCommandHandler(req: Request): Response {
        if (automateGameMasterCommands) return Response(Status.FORBIDDEN)

        logger.info("received command: ${req.bodyString()}")

        try {
            when (val command = gameMasterCommandLens(req)) {
                is GameMasterCommand.StartGame -> game.start().respondOK()
                is GameMasterCommand.RigDeck -> game.rigDeck(command.playerId, command.cards).respondOK()
                is GameMasterCommand.StartNextRound -> game.startNextRound().respondOK()
                is GameMasterCommand.StartNextTrick -> game.startNextTrick().respondOK()
            }
        } catch (e: Exception) {
            logger.error("error while executing command: ${req.bodyString()}", e)
            return Response(Status.INTERNAL_SERVER_ERROR).body(e.message ?: "unknown error")
        }

        return Response(Status.OK)
    }

    return routes(
        static(resourceLoader, "map" to ContentType.APPLICATION_JSON),
        "/" bind Method.GET to {
            Response(Status.OK).with(htmlLens of Index.withoutError)
        },
        "/play" bind Method.POST to {
            val playerId = it.form("playerId") ?: error("playerId not posted!")
            logger.info("$playerId is trying to join the game")

            try {
                game.addPlayer(playerId)
                logger.info("$playerId joined the game")
            } catch (e: GameException.PlayerWithSameNameAlreadyJoined) {
                return@to Response(Status.OK).with(htmlLens of Index.withError(e::class.simpleName!!))
            }

            val model = Game(
                wsHost = wsHost,
                players = game.players,
                waitingForMorePlayers = game.state == GameState.WaitingForMorePlayers,
                playerId = playerId,
            )
            Response(Status.OK).with(htmlLens of model)
        },
        "/do-game-master-command" bind Method.POST to ::gameMasterCommandHandler
    )
}

private fun Any.respondOK() = this.let { Response(Status.OK) }

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