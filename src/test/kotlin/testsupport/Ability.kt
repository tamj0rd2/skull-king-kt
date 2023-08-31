package testsupport

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardId
import com.tamj0rd2.domain.GamePhase
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.Hand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.Trick

interface Ability

interface ApplicationDriver {
    fun enterPlayerId(playerId: String)
    fun joinDefaultRoom()
    fun placeBet(bet: Int)
    fun playCard(cardId: CardId)

    val trickNumber: Int
    val roundNumber: Int
    val trick: Trick
    val gamePhase: GamePhase
    val gameState: GameState
    val playersInRoom: List<PlayerId>
    val hand: Hand
    val bets: Map<PlayerId, Bid>
}

interface GameMasterDriver {
    fun startGame()
    fun rigDeck(playerId: PlayerId, cards: List<Card>)
    fun startNextRound()
    fun startNextTrick()
}
