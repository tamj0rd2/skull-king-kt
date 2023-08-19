package testsupport.adapters

import App
import Driver
import PlayerId
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver
import java.lang.NullPointerException

class DomainDriver(private val app: App) : Driver {

    private lateinit var playerId: String

    override fun enterName(name: String) {
        playerId = name
    }

    override fun joinDefaultRoom() {
        app.addPlayerToRoom(playerId)
    }

    override fun isWaitingForMorePlayers(): Boolean {
        return app.waitingForMorePlayers
    }

    override fun getPlayersInRoom(): List<PlayerId> {
        return app.players
    }

    override fun hasGameStarted(): Boolean {
        return app.game != null
    }

    override fun getCardCount(name: String): Int {
        return app.game?.getCardsInHand(name)?.size ?: throw NullPointerException("game is null")
    }

    override fun startGame() = app.startGame()
}