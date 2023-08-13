package testsupport

import PlayerId

interface Ability {

}

interface ApplicationDriver {
    fun enterName(name: String)
    fun joinDefaultRoom()
    fun isWaitingForMorePlayers(): Boolean
    fun getPlayersInRoom(): List<PlayerId>
}

class AccessTheApplication(private val driver: ApplicationDriver): Ability {
    fun enterName(name: String) {
        return driver.enterName(name)
    }

    fun joinDefaultRoom() {
        return driver.joinDefaultRoom()
    }

    fun isWaitingForMorePlayers(): Boolean {
        return driver.isWaitingForMorePlayers()
    }

    fun getPlayersInRoom(): List<PlayerId> {
        return driver.getPlayersInRoom()
    }
}
