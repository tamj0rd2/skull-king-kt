package testsupport.adapters

import com.tamj0rd2.domain.Command.GameMasterCommand
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import org.eclipse.jetty.client.HttpClient
import org.http4k.client.JettyClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import testsupport.GameMasterDriver

class HTTPDriver(private val baseUrl: String, httpClient: HttpClient) : GameMasterDriver {
    private val client = JettyClient.invoke(httpClient)

    override fun perform(command: GameMasterCommand) {
        val json = command.asJsonObject().asCompactJsonString()
        val res = client(Request(Method.POST, "$baseUrl/do-game-master-command").body(json))
        if (res.status != Status.OK)
            throw RuntimeException("command failed ${command::class.simpleName}: ${res.status}\n${res.bodyString()}")
    }
}
