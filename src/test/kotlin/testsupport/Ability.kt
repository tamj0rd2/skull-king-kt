package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase

interface Ability

interface ApplicationDriver {
    fun joinGame(playerId: PlayerId)
    fun bid(bid: Int)
    fun playCard(card: Card)

    val winsOfTheRound: Map<PlayerId, Int>
    val trickWinner: PlayerId?
    val currentPlayer: PlayerId?
    val trickNumber: Int?
    val roundNumber: Int?
    val trick: List<PlayedCard>
    val roundPhase: RoundPhase?
    val gameState: GameState?
    val playersInRoom: List<PlayerId>
    val hand: List<CardWithPlayability>
    val bids: Map<PlayerId, DisplayBid>
}

interface GameMasterDriver {
    fun startGame()
    fun rigDeck(playerId: PlayerId, cards: List<Card>)
    fun startNextRound()
    fun startNextTrick()
}
