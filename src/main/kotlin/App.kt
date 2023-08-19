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
        this.gameEventSubscribers.forEach { it.handleEvent(GameEvent.PlayerJoined(playerId)) }

        if (_players.size == minRoomSizeToStartGame) _hasGameStarted = true
    }

    private val gameEventSubscribers = mutableListOf<GameEventSubscriber>()

    fun subscribeToGameEvents(subscriber: GameEventSubscriber) {
        this.gameEventSubscribers += subscriber
    }
}

fun interface GameEventSubscriber {
    fun handleEvent(event: GameEvent)
}

sealed class GameEvent {
    abstract val type: Type

    enum class Type {
        PlayerJoined
    }

    data class PlayerJoined(val playerId: PlayerId) : GameEvent() {
        override val type: Type = Type.PlayerJoined
    }
}
