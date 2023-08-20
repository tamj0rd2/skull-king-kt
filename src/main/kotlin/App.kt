typealias PlayerId = String

class Game(players: List<PlayerId>) {
    private val hands = players.associateWith { listOf(Card()) }
    private val _bets = mutableMapOf<PlayerId, Int>()
    val bets get() = _bets.toMap()

    fun getCardsInHand(playerId: PlayerId): List<Card> {
        return hands[playerId] ?: error("hand not found for player $playerId not found")
    }

    fun placeBet(playerId: PlayerId, bet: Int) {
        _bets[playerId] = bet
    }
}

class App {
    val players get(): List<PlayerId> = _players
    private val _players = mutableListOf<PlayerId>()

    private val minRoomSizeToStartGame = 2
    val waitingForMorePlayers get() = players.size < minRoomSizeToStartGame

    private var _game: Game? = null
    val game get(): Game? = _game

    fun addPlayerToRoom(playerId: PlayerId) {
        _players += playerId
        gameEventSubscribers.broadcast(GameEvent.PlayerJoined(playerId, waitingForMorePlayers))
    }

    fun startGame() {
        if (players.size < minRoomSizeToStartGame) error("not enough players to start game - ${players.size}")

        _game = Game(players)
        gameEventSubscribers.broadcast(GameEvent.GameStarted())

        gameEventSubscribers.forEach {
            it.value.handleEvent(
                GameEvent.RoundStarted(
                    _game?.getCardsInHand(it.key) ?: throw NullPointerException("game is null")
                )
            )
        }
    }

    private val gameEventSubscribers = mutableMapOf<PlayerId, GameEventSubscriber>()

    fun subscribeToGameEvents(playerId: PlayerId, subscriber: GameEventSubscriber) {
        this.gameEventSubscribers[playerId] = subscriber
    }

    private fun Map<PlayerId, GameEventSubscriber>.broadcast(event: GameEvent) {
        this.forEach { it.value.handleEvent(event) }
    }
}

class Card

fun interface GameEventSubscriber {
    fun handleEvent(event: GameEvent)
}

sealed class GameEvent {
    abstract val type: Type

    enum class Type {
        PlayerJoined,
        GameStarted,
        RoundStarted,
    }

    data class PlayerJoined(val playerId: PlayerId, val waitingForMorePlayers: Boolean) : GameEvent() {
        override val type: Type = Type.PlayerJoined
    }

    class GameStarted : GameEvent() {
        override val type: Type = Type.GameStarted
    }

    data class RoundStarted(val cardsDealt: List<Card>) : GameEvent() {
        override val type: Type = Type.RoundStarted
    }
}
