package com.tamj0rd2.domain

class Game {
    private var _phase: GamePhase = GamePhase.None
    val phase: GamePhase get() = _phase

    private var _state = GameState.WaitingForMorePlayers
    val state: GameState get() = _state

    private val _players = mutableListOf<PlayerId>()
    val players get() = _players.toList()

    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private var riggedHands: MutableMap<PlayerId, Hand>? = null

    private val isBettingComplete get() = _bets.size == players.size
    private val _bets = mutableMapOf<PlayerId, Int>()
    val bets get() = if (isBettingComplete) _bets.toMap() else emptyMap()
    val playersWhoHavePlacedBet get() = _bets.keys.toList()

    private val gameEventSubscribers = mutableMapOf<PlayerId, GameEventSubscriber>()
    private val roomSizeToStartGame = 2

    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame

    private val _currentTrick = mutableListOf<PlayedCard>()
    val currentTrick: List<PlayedCard> get() = _currentTrick

    fun addPlayer(playerId: PlayerId) {
        _players += playerId
        if (!waitingForMorePlayers) _state = GameState.WaitingToStart
        gameEventSubscribers.broadcast(GameEvent.PlayerJoined(playerId, waitingForMorePlayers))
    }

    fun start() {
        if (waitingForMorePlayers) throw GameException.NotEnoughPlayers(players.size, roomSizeToStartGame)

        _state = GameState.InProgress
        _phase = GamePhase.Bidding
        gameEventSubscribers.broadcast(GameEvent.GameStarted)

        var cardId = 0
        players.forEach { hands[it] =
            (riggedHands?.get(it) ?: listOf(Card(cardId.apply { cardId += 1 }.toString()))).toMutableList()
        }

        gameEventSubscribers.forEach {
            it.value.handleEvent(GameEvent.RoundStarted(getCardsInHand(it.key)))
        }
    }

    fun getCardsInHand(playerId: PlayerId): List<Card> {
        return hands[playerId] ?: throw GameException.NoHandFoundFor(playerId)
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

    fun playCard(playerId: String, cardId: CardId) {
        val card = getCardsInHand(playerId).find { it.id == cardId } ?: throw GameException.CardNotInHand(playerId, cardId)
        hands[playerId]?.remove(card) ?: error("the player's hand somehow doesn't exist. this should never happen")
        _currentTrick += PlayedCard(playerId, card)

        gameEventSubscribers.broadcast(GameEvent.CardPlayed(playerId, cardId))
    }

    fun rigDeck(hands: Hands) {
        this.riggedHands = hands.toMutableMap()
    }

    fun rigDeck(playerId: PlayerId, cards: List<Card>) {
        if (riggedHands == null) riggedHands = players.associateWith { emptyList<Card>() }.toMutableMap()
        riggedHands!![playerId] = cards
    }

    private fun Map<PlayerId, GameEventSubscriber>.broadcast(event: GameEvent) {
        this.forEach { it.value.handleEvent(event) }
    }
}

typealias PlayerId = String

fun interface GameEventSubscriber {
    fun handleEvent(event: GameEvent)
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

typealias Hand = List<Card>

typealias Trick = List<PlayedCard>

data class PlayedCard(val playerId: PlayerId, val card: Card)

typealias Hands = Map<PlayerId, Hand>