package testsupport.adapters

import App
import GamePhase
import GameState
import PlayerId
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(val app: App) : ApplicationDriver, GameMasterDriver {

    private lateinit var playerId: String

    override fun enterName(name: String) {
        playerId = name
    }

    override fun joinDefaultRoom() = app.game.addPlayer(playerId)

    override val playersInRoom get() = app.game.players

    override val hand get() = app.game.getCardsInHand(playerId)

    override fun placeBet(bet: Int) = app.game.placeBet(playerId, bet)
    override val gameState: GameState get() = app.game.state
    override val gamePhase: GamePhase get() = app.game.phase

    override val bets: Map<PlayerId, Int> get() = app.game.bets

    override val playersWhoHavePlacedBets: List<PlayerId> get() = app.game.playersWhoHavePlacedBet

    override fun startGame() = app.game.start()
    override fun startTrickTaking() = app.game.startTrickTaking()
}