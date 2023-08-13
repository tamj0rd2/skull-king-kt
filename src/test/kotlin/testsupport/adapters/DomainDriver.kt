package testsupport.adapters

import App
import PlayerId
import testsupport.ApplicationDriver

class DomainDriver(private val app: App) : ApplicationDriver {

    private lateinit var playerId: String

    override fun enterName(name: String) {
        playerId = name
    }

    override fun joinDefaultRoom() {
        app.addPlayerToRoom(playerId)
    }

    override fun isWaitingForMorePlayers(): Boolean {
        return app.players.size < app.minRoomSizeToStartGame
    }

    override fun getPlayersInRoom(): List<PlayerId> {
        return app.players
    }

    override fun hasGameStarted(): Boolean {
        return app.hasGameStarted
    }
}