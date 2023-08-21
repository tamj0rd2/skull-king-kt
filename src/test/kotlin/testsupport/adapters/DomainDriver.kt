package testsupport.adapters

import App
import Driver
import PlayerId
import java.lang.NullPointerException

class DomainDriver(private val app: App) : Driver {

    private lateinit var playerId: String

    override fun enterName(name: String) {
        playerId = name
    }

    override fun joinDefaultRoom() {
        app.game.addPlayer(playerId)
    }

    override fun isWaitingForMorePlayers(): Boolean {
        return app.game.waitingForMorePlayers
    }

    override fun getPlayersInRoom(): List<PlayerId> {
        return app.game.players
    }

    override fun hasGameStarted(): Boolean {
        return app.game.hasStarted
    }

    override fun getCardCount(name: String): Int {
        return app.game.getCardsInHand(name).size
    }

    override fun placeBet(bet: Int) {
        return app.game.placeBet(playerId, bet)
    }

    override fun getBets(): Map<PlayerId, Int> {
        return app.game.bets
    }

    override fun getPlayersWhoHavePlacedBets(): List<PlayerId> {
        return app.game.playersWhoHaveBet
    }

    override fun startGame() = app.game.start()
}