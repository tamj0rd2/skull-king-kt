package testsupport

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
    fun playCard(playerId: String, cardId: CardId)

    val trick: Trick
    val gamePhase: GamePhase
    val gameState: GameState
    val playersInRoom: List<PlayerId>
    val hand: Hand
    val bets: Map<PlayerId, Int>
    val playersWhoHavePlacedBets: List<PlayerId>
}

interface GameMasterDriver {
    fun startGame()
    fun rigDeck(hands: Map<PlayerId, List<Card>>)
}
