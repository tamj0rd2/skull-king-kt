typealias PlayerId = String

class Game {
    private val _players = mutableListOf<PlayerId>()
    val players get() = _players.toList()

    private val hands = mutableMapOf<PlayerId, List<Card>>()

    private val _bets = mutableMapOf<PlayerId, Int>()
    private val isBettingComplete get() = _bets.size == players.size
    val bets get() = if (isBettingComplete) _bets.toMap() else emptyMap()
    val playersWhoHaveBet get() = _bets.keys.toList()

    private val gameEventSubscribers = mutableMapOf<PlayerId, GameEventSubscriber>()
    private val minRoomSizeToStartGame = 2

    val waitingForMorePlayers get() = players.size < minRoomSizeToStartGame
    private var _hasStarted = false
    val hasStarted get() = _hasStarted

    fun addPlayer(playerId: PlayerId) {
        _players += playerId
        gameEventSubscribers.broadcast(GameEvent.PlayerJoined(playerId, waitingForMorePlayers))
    }

    fun start() {
        if (players.size < minRoomSizeToStartGame) error("not enough players to start game - ${players.size}")

        _hasStarted = true
        gameEventSubscribers.broadcast(GameEvent.GameStarted())

        players.forEach { hands[it] = listOf(Card()) }
        gameEventSubscribers.forEach {
            it.value.handleEvent(GameEvent.RoundStarted(getCardsInHand(it.key)))
        }
    }

    fun getCardsInHand(playerId: PlayerId): List<Card> {
        return hands[playerId] ?: error("hand not found for player $playerId not found")
    }

    fun placeBet(playerId: PlayerId, bet: Int) {
        _bets[playerId] = bet
        this.gameEventSubscribers.broadcast(GameEvent.BetPlaced(playerId))

        if (isBettingComplete)
            this.gameEventSubscribers.broadcast(GameEvent.BettingCompleted(bets))
    }

    fun subscribeToGameEvents(playerId: PlayerId, subscriber: GameEventSubscriber) {
        this.gameEventSubscribers[playerId] = subscriber
    }

    private fun Map<PlayerId, GameEventSubscriber>.broadcast(event: GameEvent) {
        this.forEach { it.value.handleEvent(event) }
    }

    fun startTrickTaking() {
    }
}

class App {
    val game = Game()
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
        BetPlaced,
        BettingCompleted,
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

    data class BetPlaced(val playerId: PlayerId) : GameEvent() {
        override val type: Type = Type.BetPlaced
    }

    data class BettingCompleted(val bets: Map<PlayerId, Int>) : GameEvent() {
        override val type: Type = Type.BettingCompleted
    }
}
