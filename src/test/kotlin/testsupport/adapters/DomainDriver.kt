package testsupport.adapters

import App
import Driver
import GameState
import PlayerId

class DomainDriver(val app: App) : Driver {

    private lateinit var playerId: String

    override fun enterName(name: String) {
        playerId = name
    }

    override fun joinDefaultRoom() = app.game.addPlayer(playerId)

    override val playersInRoom get() = app.game.players

    override val cardCount get() = app.game.getCardsInHand(playerId).size

    override fun placeBet(bet: Int) = app.game.placeBet(playerId, bet)
    override val gameState: GameState get() = app.game.state

    override val bets: Map<PlayerId, Int>
        get() = app.game.bets

    override val playersWhoHavePlacedBets: List<PlayerId>
        get() = app.game.playersWhoHavePlacedBet

    override fun startGame() = app.game.start()
    override fun startTrickTaking() = app.game.startTrickTaking()
}