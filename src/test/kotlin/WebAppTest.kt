import com.tamj0rd2.webapp.wsHandler
import org.http4k.server.Jetty
import org.http4k.server.asServer
import testsupport.adapters.WebDriver

private const val port = 9001
class WebAppTest : AppTestContract({ app ->
    wsHandler(app).asServer(Jetty(port)).start().use {
        WebDriver(app, port)
    }
})