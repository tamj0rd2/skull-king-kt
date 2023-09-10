package com.tamj0rd2.domain

class Game {
    private var _trickNumber: Int = 0
    val trickNumber: Int get() = _trickNumber

    private var _roundNumber = 0
    val roundNumber: Int get() = _roundNumber

    private var _phase: RoundPhase? = null
    val phase: RoundPhase get() = _phase ?: error("game not started")

    private var _state = GameState.WaitingForMorePlayers
    val state: GameState get() = _state

    private val _players = mutableListOf<PlayerId>()
    val players get() = _players.toList()

    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private var riggedHands: MutableMap<PlayerId, Hand>? = null

    private val _bids = Bids()
    val bids: Map<PlayerId, Bid> get() = _bids.forDisplay()

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
        require(!waitingForMorePlayers) { "not enough players to start the game - ${players.size}/$roomSizeToStartGame" }

        _state = GameState.InProgress
        players.forEach { hands[it] = mutableListOf() }
        gameEventSubscribers.broadcast(GameEvent.GameStarted(players))
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

    fun getCardsInHand(playerId: PlayerId): List<Card> = getHandFor(playerId)

    private fun getHandFor(playerId: PlayerId): MutableList<Card> {
        val hand = hands[playerId]
        requireNotNull(hand) { "player $playerId somehow doesn't have a hand" }
        return hand
    }

    fun bid(playerId: PlayerId, bid: Int) {
        if (state != GameState.InProgress) throw GameException.CannotBid("game not in progress")
        if (phase != RoundPhase.Bidding) throw GameException.CannotBid("not in bidding phase")

        _bids.place(playerId, bid)
        this.gameEventSubscribers.broadcast(GameEvent.BidPlaced(playerId))

        if (_bids.areComplete) {
            this._phase = RoundPhase.TrickTaking
            this.gameEventSubscribers.broadcast(GameEvent.BiddingCompleted(_bids.asCompleted()))
        }
    }

    fun subscribeToGameEvents(playerId: PlayerId, subscriber: GameEventSubscriber) {
        this.gameEventSubscribers[playerId] = subscriber
    }

    fun playCard(playerId: PlayerId, cardId: CardId) {
        val hand = getHandFor(playerId)
        val card = hand.find { it.id == cardId }
        requireNotNull(card) { "card $cardId not in $playerId's hand" }

        hand.remove(card)
        _currentTrick += PlayedCard(playerId, card)
        gameEventSubscribers.broadcast(GameEvent.CardPlayed(playerId, cardId))

        if (_currentTrick.size == players.size) {
            _phase = RoundPhase.TrickComplete
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
        _phase = RoundPhase.Bidding
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

enum class RoundPhase {
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
        if (bids[playerId] !is Bid.None) throw GameException.CannotBid("player $playerId has already bid")

        bids[playerId] = Bid.Placed(bid)
    }

    fun asCompleted(): Map<PlayerId, Int> {
        return bids.mapValues {
            require(it.value !is Bid.None)
            when (val bid = it.value) {
                is Bid.None -> error("not all players have bid")
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
