package com.tamj0rd2.domain

typealias PlayerId = String

typealias Hand = List<Card>

class Game {
    private var _phase: GamePhase = GamePhase.None
    val phase: GamePhase get() = _phase

    private val _players = mutableListOf<PlayerId>()
    val players get() = _players.toList()

    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private val isBettingComplete get() = _bets.size == players.size

    private val _bets = mutableMapOf<PlayerId, Int>()
    val bets get() = if (isBettingComplete) _bets.toMap() else emptyMap()
    val playersWhoHavePlacedBet get() = _bets.keys.toList()

    private val gameEventSubscribers = mutableMapOf<PlayerId, GameEventSubscriber>()
    private val roomSizeToStartGame = 2

    private var _state = GameState.WaitingForMorePlayers
    val state: GameState get() = _state

    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame

    private val _currentTrick = mutableListOf<PlayedCard>()
    val currentTrick: List<PlayedCard> get() = _currentTrick

    private var riggedHands: Map<PlayerId, Hand>? = null

    fun addPlayer(playerId: PlayerId) {
        _players += playerId
        if (!waitingForMorePlayers) _state = GameState.WaitingToStart
        gameEventSubscribers.broadcast(GameEvent.PlayerJoined(playerId, waitingForMorePlayers))
    }

    fun start() {
        if (waitingForMorePlayers) error("not enough players to start game - ${players.size}/$roomSizeToStartGame")

        _state = GameState.InProgress
        _phase = GamePhase.Bidding
        gameEventSubscribers.broadcast(GameEvent.GameStarted())

        var cardId = 0
        players.forEach { hands[it] =
            (riggedHands?.get(it) ?: listOf(Card(cardId.apply { cardId += 1 }.toString()))).toMutableList()
        }

        gameEventSubscribers.forEach {
            it.value.handleEvent(GameEvent.RoundStarted(getCardsInHand(it.key)))
        }
    }

    fun getCardsInHand(playerId: PlayerId): List<Card> {
        return hands[playerId] ?: error("hand not found for player $playerId not found")
    }

    fun placeBet(playerId: PlayerId, bet: Int) {
        _bets[playerId] = bet
        this.gameEventSubscribers.broadcast(GameEvent.BetPlaced(playerId, isBettingComplete))

        if (isBettingComplete) {
            this._phase = GamePhase.TrickTaking
            this.gameEventSubscribers.broadcast(GameEvent.BettingCompleted(bets))
        }
    }

    fun subscribeToGameEvents(playerId: PlayerId, subscriber: GameEventSubscriber) {
        this.gameEventSubscribers[playerId] = subscriber
    }

    private fun Map<PlayerId, GameEventSubscriber>.broadcast(event: GameEvent) {
        this.forEach { it.value.handleEvent(event) }
    }

    fun playCard(playerId: String, cardId: CardId) {
        // TODO: I should start extracting these specific error codes out. I wrote the same thing in the webdriver earlier
        val card = getCardsInHand(playerId).find { it.id == cardId } ?: error("card $cardId not found in $playerId's hand")
        hands[playerId]?.remove(card) ?: error("the player's hand somehow doesn't exist...")
        _currentTrick += PlayedCard(playerId, card)

        gameEventSubscribers.broadcast(GameEvent.CardPlayed(playerId, cardId))
    }

    fun rigDeck(hands: Hands) {
        this.riggedHands = hands
    }
}

class App {
    val game = Game()
}

data class Card(val id: CardId)

typealias CardId = String

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
        CardPlayed,
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

    data class BetPlaced(val playerId: PlayerId, val isBettingComplete: Boolean) : GameEvent() {
        override val type: Type = Type.BetPlaced
    }

    data class BettingCompleted(val bets: Map<PlayerId, Int>) : GameEvent() {
        override val type: Type = Type.BettingCompleted
    }

    data class CardPlayed(val playerId: String, val cardId: CardId) : GameEvent() {
        override val type: Type = Type.CardPlayed
    }
}

enum class GameState {
    WaitingForMorePlayers,
    WaitingToStart,
    InProgress,
}

enum class GamePhase {
    None,
    Bidding,
    TrickTaking,
}

typealias Trick = List<PlayedCard>

data class PlayedCard(val playerId: PlayerId, val card: Card)
typealias Hands = Map<PlayerId, Hand>