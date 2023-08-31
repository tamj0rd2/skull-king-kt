package com.tamj0rd2.domain

class Game {
    private var _trickNumber: Int = 0
    val trickNumber: Int get() = _trickNumber

    private var _roundNumber = 0
    val roundNumber: Int get() = _roundNumber

    private var _phase: GamePhase? = null
    val phase: GamePhase get() = _phase ?: throw GameException.NotStarted()

    private var _state = GameState.WaitingForMorePlayers
    val state: GameState get() = _state

    private val _players = mutableListOf<PlayerId>()
    val players get() = _players.toList()

    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private var riggedHands: MutableMap<PlayerId, Hand>? = null

    private val _bids = Bids()
    val bets: Map<PlayerId, Bid> get() = _bids.forDisplay()

    private val gameEventSubscribers = mutableMapOf<PlayerId, GameEventSubscriber>()
    private val roomSizeToStartGame = 2

    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame

    private val _currentTrick = mutableListOf<PlayedCard>()
    val currentTrick: List<PlayedCard> get() = _currentTrick

    fun addPlayer(playerId: PlayerId) {
        if (_players.contains(playerId)) throw GameException.PlayerWithSameNameAlreadyJoined(playerId)

        _players += playerId
        if (!waitingForMorePlayers) _state = GameState.WaitingToStart
        gameEventSubscribers.broadcast(GameEvent.PlayerJoined(playerId, waitingForMorePlayers))
    }

    fun start() {
        if (waitingForMorePlayers) throw GameException.NotEnoughPlayers(players.size, roomSizeToStartGame)

        _state = GameState.InProgress
        players.forEach { hands[it] = mutableListOf() }
        gameEventSubscribers.broadcast(GameEvent.GameStarted)
        startNextRound()
    }

    private fun dealCards() {
        val deck = (1..66).map { Card(it.toString()) }.toMutableList()

        hands.replaceAll { playerId, _ ->
            val riggedHand = riggedHands?.get(playerId)
            if (riggedHand != null) {
                return@replaceAll riggedHand.toMutableList()
            }

            (1..roundNumber).map { deck.removeFirst() }.toMutableList()
        }
    }

    fun getCardsInHand(playerId: PlayerId): List<Card> {
        return hands[playerId] ?: throw GameException.NoHandFoundFor(playerId)
    }

    fun placeBet(playerId: PlayerId, bid: Int) {
        _bids.place(playerId, bid)
        this.gameEventSubscribers.broadcast(GameEvent.BetPlaced(playerId))

        if (_bids.areComplete) {
            this._phase = GamePhase.TrickTaking
            this.gameEventSubscribers.broadcast(GameEvent.BettingCompleted(_bids.asCompleted()))
        }
    }

    fun subscribeToGameEvents(playerId: PlayerId, subscriber: GameEventSubscriber) {
        this.gameEventSubscribers[playerId] = subscriber
    }

    fun playCard(playerId: String, cardId: CardId) {
        val cards = getCardsInHand(playerId)
        val card = cards.find { it.id == cardId } ?: throw GameException.CardNotInHand(playerId, cardId)
        hands[playerId]?.remove(card) ?: error("the player's hand somehow doesn't exist. this should never happen")
        _currentTrick += PlayedCard(playerId, card)
        gameEventSubscribers.broadcast(GameEvent.CardPlayed(playerId, cardId))

        if (_currentTrick.size == players.size) {
            _phase = GamePhase.TrickComplete
            gameEventSubscribers.broadcast(GameEvent.TrickCompleted)

            if (roundNumber == 10) {
                _state = GameState.Complete
                gameEventSubscribers.broadcast(GameEvent.GameCompleted)
            }
        }
    }

    fun rigDeck(playerId: PlayerId, cards: List<Card>) {
        if (riggedHands == null) riggedHands = players.associateWith { emptyList<Card>() }.toMutableMap()
        riggedHands!![playerId] = cards
    }

    private fun Map<PlayerId, GameEventSubscriber>.broadcast(event: GameEvent) {
        this.forEach { it.value.handleEvent(event) }
    }

    fun startNextRound() {
        _roundNumber += 1
        _trickNumber = 0
        _currentTrick.clear()
        _bids.initFor(players)
        _phase = GamePhase.Bidding
        dealCards()

        gameEventSubscribers.forEach {
            it.value.handleEvent(GameEvent.RoundStarted(getCardsInHand(it.key), roundNumber))
        }
    }

    fun startNextTrick() {
        _trickNumber += 1
        _currentTrick.clear()

        gameEventSubscribers.forEach {
            it.value.handleEvent(GameEvent.TrickStarted(trickNumber))
        }
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
    Complete,
}

enum class GamePhase {
    Bidding,
    TrickTaking,
    TrickComplete,
}

typealias Hand = List<Card>

typealias Trick = List<PlayedCard>

data class PlayedCard(val playerId: PlayerId, val card: Card) {
    override fun toString(): String {
        return "$card played by $playerId"
    }
}

private class Bids {
    private var bids = mutableMapOf<PlayerId, Bid>()

    val areComplete get() = bids.none { it.value is Bid.None }

    fun initFor(players: Collection<PlayerId>) {
        bids = players.associateWith { Bid.None }.toMutableMap()
    }

    fun forDisplay(): Map<PlayerId, Bid> = when {
        areComplete -> bids
        else -> bids.mapValues { if (it.value is Bid.Placed) Bid.IsHidden else it.value }
    }

    fun place(playerId: PlayerId, bid: Int) {
        bids[playerId] = Bid.Placed(bid)
    }

    fun asCompleted(): Map<PlayerId, Int> {
        return bids.mapValues {
            when (val bid = it.value) {
                is Bid.None -> throw GameException.NotAllPlayersHaveBid()
                is Bid.IsHidden -> error("this should be impossible. this is just for display")
                is Bid.Placed -> bid.bid
            }
        }
    }
}

sealed class Bid {
    override fun toString(): String = when (this) {
        is None -> "None"
        is IsHidden -> "Hidden"
        is Placed -> bid.toString()
    }

    object None : Bid()
    object IsHidden : Bid()

    data class Placed(val bid: Int) : Bid()
}
