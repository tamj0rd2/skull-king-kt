package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.RigDeckCommand
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.format.Jackson.asCompactJsonString
import org.http4k.format.Jackson.asJsonObject
import testsupport.GameMasterDriver

class HTTPDriver(private val baseUrl: String, httpClient: HttpClient) : GameMasterDriver {
    private val client = JettyClient.invoke(httpClient)

    override fun startGame() {
        val res = client(Request(Method.POST, "$baseUrl/startGame"))
        if (res.status != Status.OK) throw RuntimeException("failed to start game")
    }

    override fun rigDeck(hands: Map<PlayerId, List<Card>>) {
        val json = RigDeckCommand(hands).asJsonObject().asCompactJsonString()
        val res = client(Request(Method.PUT, "$baseUrl/rigDeck").body(json))
        if (res.status != Status.OK) throw RuntimeException("failed to rig deck: ${res.status}\n${res.bodyString()}")
    }
}