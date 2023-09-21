package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.DeprecatedBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.Trick

interface Ability

interface ApplicationDriver {
    fun joinGame(playerId: PlayerId)
    fun bid(bid: Int)
    fun playCard(card: Card)

    val currentPlayer: PlayerId?
    val trickNumber: Int
    val roundNumber: Int
    val trick: Trick
    val roundPhase: RoundPhase
    val gameState: GameState
    val playersInRoom: List<PlayerId>
    val hand: Hand
    val bids: Map<PlayerId, DeprecatedBid>
}

interface GameMasterDriver {
    fun startGame()
    fun rigDeck(playerId: PlayerId, cards: List<Card>)
    fun startNextRound()
    fun startNextTrick()
}
