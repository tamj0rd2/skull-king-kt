package testsupport

import PlayerId

interface Ability

interface ApplicationDriver {
    fun enterName(name: String)
    fun joinDefaultRoom()
    fun placeBet(bet: Int)

    val isWaitingForMorePlayers: Boolean
    val playersInRoom: List<PlayerId>
    val hasGameStarted: Boolean
    val cardCount: Int
    val bets: Map<PlayerId, Int>
    val playersWhoHavePlacedBets: List<PlayerId>
}

interface GameMasterDriver {
    fun startGame()
    fun startTrickTaking()
}

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver
