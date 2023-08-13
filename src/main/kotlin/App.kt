typealias PlayerId = String

class App {
    val players = mutableListOf<PlayerId>()

    val minRoomSizeToStartGame = 2

    val hasGameStarted get() = _hasGameStarted
    private var _hasGameStarted = false

    fun addPlayerToRoom(playerId: PlayerId) {
        players += playerId

        if (players.size == minRoomSizeToStartGame) _hasGameStarted = true
    }
}