package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Command.GameMasterCommand
import com.tamj0rd2.domain.Game
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
import org.http4k.core.with
import org.http4k.format.auto
import org.http4k.lens.ContentNegotiation
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import org.slf4j.LoggerFactory

private data class Game(
    val host: String,
    // TODO: might be able to get rid of this since the data is coming in the YouJoined message
    val playersJson: String,
    val waitingForMorePlayers: Boolean,
    val playerId: String,
) : ViewModel {

    constructor(
        host: String,
        players: List<PlayerId>,
        waitingForMorePlayers: Boolean,
        playerId: PlayerId,
    ) : this(
        host = host,
        playersJson = players.asJsonObject().asCompactJsonString(),
        waitingForMorePlayers = waitingForMorePlayers,
        playerId = playerId.playerId,
    )
}

private data class Index(val errorMessage: String? = null) : ViewModel {
    companion object {
        val withoutError = Index()
    }
}

internal fun httpHandler(
    game: Game,
    hotReload: Boolean,
    automateGameMasterCommands: Boolean
): HttpHandler {
    val logger = LoggerFactory.getLogger("httpHandler")
    val (renderer, resourceLoader) = buildResourceLoaders(hotReload)
    val gameMasterCommandLens = Body.auto<GameMasterCommand>().toLens()

    val vmHtmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
    val vmJsonLens = Body.auto<ViewModel>().toLens()
    val negotiator = ContentNegotiation.auto(vmHtmlLens, vmJsonLens)

    fun gameMasterCommandHandler(req: Request): Response {
        if (automateGameMasterCommands) return Response(Status.FORBIDDEN)

        val command = gameMasterCommandLens(req)
        logger.info("received command: $command")

        return runCatching { game.perform(command) }
            .fold(
                onSuccess = { Response(Status.OK) },
                onFailure = {e ->
                    logger.error("error while executing command: ${req.bodyString()}", e)
                    return Response(Status.INTERNAL_SERVER_ERROR).body(e.message ?: "unknown error")
                }
            )
    }

    return routes(
        static(resourceLoader, "map" to ContentType.APPLICATION_JSON),
        "/" bind Method.GET to {
            Response(Status.OK).with(negotiator.outbound(it) of Index.withoutError)
        },
        "/do-game-master-command" bind Method.POST to ::gameMasterCommandHandler
    )
}

private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}