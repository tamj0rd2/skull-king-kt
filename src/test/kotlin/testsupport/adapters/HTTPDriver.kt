package testsupport.adapters

import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.openqa.selenium.By
import testsupport.GameMasterDriver

class HTTPDriver(private val baseUrl: String, httpClient: HttpClient) : GameMasterDriver {
    private val client = JettyClient.invoke(httpClient)

    override fun startGame() {
        val res = client(Request(Method.POST, "$baseUrl/startGame"))
        if (res.status != Status.OK) throw RuntimeException("failed to start game")
    }

    override fun startTrickTaking() = TODO("Not yet implemented")
}