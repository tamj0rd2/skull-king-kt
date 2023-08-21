import com.tamj0rd2.webapp.httpHandler
import com.tamj0rd2.webapp.wsHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.Undertow
import org.http4k.server.asServer

object WebServer {
    @JvmStatic fun main(args: Array<String>) {
        make(8080, hotReload = true).start()
    }

    fun make(port: Int, hotReload: Boolean = false): Http4kServer {
        val app = App()
        val http = httpHandler(port, hotReload, app)
        val ws = wsHandler(app)
        return PolyHandler(http, ws).asServer(Undertow(port))
    }
}
