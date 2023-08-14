typealias PlayerId = String

class App {
    val players get(): List<PlayerId> = _players
    private val _players = mutableListOf<PlayerId>()

    private val minRoomSizeToStartGame = 2
    val waitingForMorePlayers get() = players.size < minRoomSizeToStartGame

    val hasGameStarted get() = _hasGameStarted
    private var _hasGameStarted = false

    fun addPlayerToRoom(playerId: PlayerId) {
        _players += playerId

        if (_players.size == minRoomSizeToStartGame) _hasGameStarted = true
    }
}