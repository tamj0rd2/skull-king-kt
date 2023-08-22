package testsupport

import GameState
import Hand
import PlayerId

interface Ability

interface ApplicationDriver {
    fun enterName(name: String)
    fun joinDefaultRoom()
    fun placeBet(bet: Int)

    val gameState: GameState
    val playersInRoom: List<PlayerId>
    val hand: Hand
    val bets: Map<PlayerId, Int>
    val playersWhoHavePlacedBets: List<PlayerId>
}

interface GameMasterDriver {
    fun startGame()
    fun startTrickTaking()
}

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver
