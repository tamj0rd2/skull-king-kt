package testsupport

import PlayerId

interface Ability

interface ApplicationDriver : Ability {
    fun enterName(name: String)
    fun joinDefaultRoom()
    fun isWaitingForMorePlayers(): Boolean
    fun getPlayersInRoom(): List<PlayerId>
    fun hasGameStarted(): Boolean
}

class AccessTheApplication(private val driver: ApplicationDriver): Ability, ApplicationDriver by driver
