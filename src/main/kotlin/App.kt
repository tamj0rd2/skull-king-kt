typealias PlayerId = String

class App {
    val players = mutableListOf<PlayerId>()

    val minRoomSizeToStartGame = 2

    fun addPlayerToRoom(playerId: PlayerId) {
        players += playerId
    }
}