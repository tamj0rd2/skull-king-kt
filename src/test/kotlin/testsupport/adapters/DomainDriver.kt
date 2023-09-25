package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(private val game: Game) : ApplicationDriver, GameMasterDriver {

    private lateinit var playerId: PlayerId

    override fun joinGame(playerId: PlayerId) {
        this.playerId = playerId
        game.addPlayer(playerId)
    }
    override fun startGame() {
        game.start()
    }
    override fun rigDeck(playerId: PlayerId, cards: List<Card>) = game.rigDeck(playerId, cards)
    override fun startNextRound() {
        game.startNextRound()
    }
    override fun startNextTrick() {
        game.startNextTrick()
    }

    override fun bid(bid: Int) {
        game.bid(playerId, bid)
    }

    override fun playCard(card: Card) {
        game.playCard(playerId, card.name)
    }

    override fun isCardPlayable(card: Card): Boolean {
        return game.isCardPlayable(playerId, card)
    }

    override val winsOfTheRound: Map<PlayerId, Int>
        get() = game.winsOfTheRound

    override val trickWinner: PlayerId? get() = game.trickWinner

    override val playersInRoom get() = game.players
    override val hand get() = game.getCardsInHand(playerId)
    override val trick: List<PlayedCard> get() = game.currentTrick.playedCards
    override val gameState: GameState? get() = game.state
    override val roundPhase: RoundPhase? get() = game.phase
    override val bids: Map<PlayerId, DisplayBid> get() = game.bids
    override val trickNumber: Int? get() = game.trickNumber
    override val roundNumber: Int? get() = game.roundNumber
    override val currentPlayer: PlayerId? get() = game.currentPlayersTurn
}