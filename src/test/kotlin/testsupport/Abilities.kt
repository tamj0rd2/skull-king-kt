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
}

class ParticipateInGames(private val driver: ApplicationDriver): Ability, ApplicationDriver by driver
