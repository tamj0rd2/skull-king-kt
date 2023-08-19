package com.tamj0rd2.webapp

import App
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.asServer
import java.time.Clock

fun main() {
    //if setting this to true, remember to run the app with the working directory set to the root of the example
    val hotReload = true
    val clock = Clock.systemDefaultZone()

    val port = 8080
    val app = App()
    val http = httpHandler(port, clock, hotReload, app)
    val ws = wsHandler(app)
    PolyHandler(http, ws).asServer(Jetty(port)).start()
}
