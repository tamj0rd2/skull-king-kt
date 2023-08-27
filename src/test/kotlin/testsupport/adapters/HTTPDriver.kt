package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.webapp.GameMasterCommand
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
        // TODO: make this a GM command too
        val res = client(Request(Method.POST, "$baseUrl/startGame"))
        if (res.status != Status.OK) throw RuntimeException("failed to start game")
    }

    override fun rigDeck(playerId: PlayerId, cards: List<Card>) = doCommand(GameMasterCommand.RigDeck(playerId, cards))

    override fun startNextRound() = doCommand(GameMasterCommand.StartRound)

    override fun startNextTrick() {
        TODO("Not yet implemented")
    }

    private fun doCommand(command: GameMasterCommand) {
        val json = command.asJsonObject().asCompactJsonString()
        val res = client(Request(Method.POST, "$baseUrl/do-game-master-command").body(json))
        if (res.status != Status.OK)
            throw RuntimeException("command failed ${command::class.simpleName}: ${res.status}\n${res.bodyString()}")
    }
}
