package testsupport

import Card
import CardId
import GamePhase
import GameState
import Hand
import PlayerId
import Trick

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
