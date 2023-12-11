package com.tamj0rd2.webapp

import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.webapp.CustomJsonSerializer.auto
import com.tamj0rd2.webapp.Frontend.*
import org.http4k.client.JettyClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
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

private data class Play(val host: String, val ackTimeoutMs: Long) : ViewModel
private data class PlaySvelte(val host: String, val ackTimeoutMs: Long, val devServer: Boolean) : ViewModel
private data class PlaySolid(val host: String, val ackTimeoutMs: Long, val devServer: Boolean) : ViewModel

enum class Frontend(val usesViteInDevMode: Boolean) {
    WebComponents(usesViteInDevMode = false),
    Svelte(usesViteInDevMode = true),
    Solid(usesViteInDevMode = true),
}

internal fun httpHandler(
    game: Game,
    host: String,
    devServer: Boolean,
    automateGameMasterCommands: Boolean,
    ackTimeoutMs: Long,
    frontend: Frontend,
): HttpHandler {
    val logger = LoggerFactory.getLogger("httpHandler")
    val (renderer, resourceLoader) = buildResourceLoaders(devServer)
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
                onFailure = { e ->
                    logger.error("error while executing command: ${req.bodyString()}", e)
                    return Response(Status.INTERNAL_SERVER_ERROR).body(e.message ?: "unknown error")
                }
            )
    }

    val router = routes(
        static(resourceLoader, "map" to ContentType.APPLICATION_JSON),
        "/" bind Method.GET to {
            Response(Status.MOVED_PERMANENTLY).with(Header.LOCATION of Uri.of("/play"))
        },
        "/play" bind Method.GET to {
            val vm = when (frontend) {
                WebComponents -> Play(host, ackTimeoutMs)
                Svelte -> PlaySvelte(host, ackTimeoutMs, devServer)
                Solid -> PlaySolid(host, ackTimeoutMs, devServer)
            }
            Response(Status.OK).with(negotiator.outbound(it) of vm)
        },
        "/do-game-master-command" bind Method.POST to ::gameMasterCommandHandler
    )

    val loggingFilter = Filter { next ->
        { request ->
            next(request).also { response ->
                if (response.status.successful || response.status.redirection)
                    logger.trace("Responded with {} for {} {}", response.status, request.method, request.uri)
                else logger.warn("Responded with {} for {} {}", response.status, request.method, request.uri)
            }
        }
    }

    return loggingFilter
        .then(if (devServer && frontend.usesViteInDevMode) frontend.viteProxy() else Filter.NoOp)
        .then(router)
}

private fun Frontend.viteProxy(): Filter {
    val viteHttpClient = JettyClient()
    val vitePort = when(this) {
        WebComponents -> error("web components frontend does not use vite")
        Svelte -> 5174
        Solid -> 5173
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

private fun buildResourceLoaders(hotReload: Boolean) = when {
    hotReload -> HandlebarsTemplates().HotReload("./src/main/resources") to ResourceLoader.Classpath("public")
    else -> HandlebarsTemplates().CachingClasspath() to ResourceLoader.Classpath("public")
}