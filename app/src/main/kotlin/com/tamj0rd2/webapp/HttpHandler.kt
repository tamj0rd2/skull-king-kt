package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.webapp.CustomJsonSerializer.auto
import com.tamj0rd2.webapp.Frontend.Svelte
import org.http4k.client.JettyClient
import org.http4k.core.*
import org.http4k.core.HttpHandler
import org.http4k.format.auto
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Header
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import org.slf4j.LoggerFactory

private data class PlaySvelte(val host: String, val ackTimeoutMs: Long, val devServer: Boolean) : ViewModel

enum class Frontend(val usesViteInDevMode: Boolean) {
    Svelte(usesViteInDevMode = true),
}

internal class HttpHandler(
    private val game: Game,
    host: String,
    devServer: Boolean,
    private val automateGameMasterCommands: Boolean,
    ackTimeoutMs: Long,
    frontend: Frontend,
) : HttpHandler {
    private val logger = LoggerFactory.getLogger("httpHandler")
    private val renderer =
        if (devServer) HandlebarsTemplates().HotReload("./src/main/resources") else HandlebarsTemplates().CachingClasspath()
    private val resourceLoader = ResourceLoader.Classpath("public")

    private val gameMasterCommandLens = Body.auto<GameMasterCommand>().toLens()
    private val vmHtmlLens = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()
    private val vmJsonLens = Body.auto<ViewModel>().toLens()
    private val negotiator = ContentNegotiation.auto(vmHtmlLens, vmJsonLens)

    override fun invoke(request: Request): Response = handler.invoke(request)

    private val loggingFilter = Filter { next ->
        { request ->
            next(request).also { response ->
                if (response.status.successful || response.status.redirection)
                    logger.trace("Responded with {} for {} {}", response.status, request.method, request.uri)
                else logger.warn("Responded with {} for {} {}", response.status, request.method, request.uri)
            }
        }
    }

    private val router = routes(
        static(resourceLoader, "map" to ContentType.APPLICATION_JSON),
        "/" bind Method.GET to {
            Response(Status.MOVED_PERMANENTLY).with(Header.LOCATION of Uri.of("/play"))
        },
        "/play" bind Method.GET to {
            val vm = when (frontend) {
                Svelte -> PlaySvelte(host, ackTimeoutMs, devServer)
            }
            Response(Status.OK).with(negotiator.outbound(it) of vm)
        },
        "/do-game-master-command" bind Method.POST to ::gameMasterCommandHandler
    )

    private fun gameMasterCommandHandler(req: Request): Response {
        if (automateGameMasterCommands) return Response(Status.FORBIDDEN)

        val command = gameMasterCommandLens(req)
        logger.info("received command: $command")

        return runCatching { game.perform(command) }
            .fold(
                onSuccess = { Response(Status.OK) },
                onFailure = { e ->
                    logger.error("error while executing command: ${req.bodyString()}", e)
                    return Response(Status.INTERNAL_SERVER_ERROR).body(e.message ?: "unknown error")
                }
            )
    }

    private val handler = loggingFilter
        .then(if (devServer && frontend.usesViteInDevMode) frontend.viteProxy() else Filter.NoOp)
        .then(router)
}

private fun Frontend.viteProxy(): Filter {
    val viteHttpClient = JettyClient()
    val vitePort = when (this) {
        Svelte -> 5174
    }

    return Filter { next ->
        { req ->
            val response = next(req)
            if (response.status == Status.NOT_FOUND && req.method == Method.GET) {
                val proxiedRequest = Request(Method.GET, Uri.of("http://localhost:$vitePort" + req.uri.path))
                viteHttpClient(proxiedRequest)
            } else {
                response
            }
        }
    }
}
