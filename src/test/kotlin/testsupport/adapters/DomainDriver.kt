package testsupport.adapters

import com.tamj0rd2.domain.App
import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GamePhase
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.Trick
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(private val app: App) : ApplicationDriver, GameMasterDriver {

    private lateinit var playerId: String

    override val playersInRoom get() = app.game.players

    override val hand get() = app.game.getCardsInHand(playerId)
    override val trick: Trick get() = app.game.currentTrick
    override val gameState: GameState get() = app.game.state
    override val gamePhase: GamePhase get() = app.game.phase

    override val bets: Map<PlayerId, Bid> get() = app.game.bets

    override fun joinGame(playerId: PlayerId) {
        this.playerId = playerId
        app.game.addPlayer(playerId)
    }

    override fun placeBet(bet: Int) = app.game.placeBet(playerId, bet)
    override fun playCard(cardId: CardId) = app.game.playCard(playerId, cardId)
    override val trickNumber: Int get() = app.game.trickNumber
    override val roundNumber: Int get() = app.game.roundNumber

    override fun startGame() = app.game.start()
    override fun rigDeck(playerId: PlayerId, cards: List<Card>) = app.game.rigDeck(playerId, cards)
    override fun startNextRound() = app.game.startNextRound()
    override fun startNextTrick() = app.game.startNextTrick()
}