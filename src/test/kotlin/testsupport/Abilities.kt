package testsupport

import PlayerId

interface Ability

interface ApplicationDriver {
    fun enterName(name: String)
    fun joinDefaultRoom()
    fun isWaitingForMorePlayers(): Boolean
    fun getPlayersInRoom(): List<PlayerId>
    fun hasGameStarted(): Boolean
    fun getCardCount(name: String): Int
    fun placeBet(bet: Int)
    fun getBets(): Map<PlayerId, Int>
}

interface GameMasterDriver {
    fun startGame()
}

class ParticipateInGames(private val driver: ApplicationDriver): Ability, ApplicationDriver by driver
class StartGames(private val driver: GameMasterDriver): Ability, GameMasterDriver by driver
