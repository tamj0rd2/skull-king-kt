package com.tamj0rd2.domain

import com.tamj0rd2.domain.RoundPhase.*

fun interface GameEventListener {
    fun handle(event: GameEvent)
}

class Game {
    private var _trickWinner: PlayerId? = null
    val trickWinner: PlayerId? get() = _trickWinner

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
    val bids: Map<PlayerId, DeprecatedBid> get() = _bids.forDisplay()

    private val roomSizeToStartGame = 2

    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame

    private lateinit var trick: Trick
    val currentTrick: List<PlayedCard> get() = trick.playedCards

    private var roundTurnOrder = mutableListOf<PlayerId>()
    val currentPlayersTurn get(): PlayerId? = roundTurnOrder.firstOrNull()

    fun isInState(state: GameState) = this.state == state

    private val eventListeners = mutableListOf<GameEventListener>()
    fun subscribeToGameEvents(listener: GameEventListener) {
        this.eventListeners += listener
    }

    fun addPlayer(playerId: PlayerId) {
        if (_players.contains(playerId)) throw GameException.PlayerWithSameNameAlreadyJoined(playerId)

        _players += playerId
        if (!waitingForMorePlayers) _state = GameState.WaitingToStart
        recordEvent(GameEvent.PlayerJoined(playerId))
    }

    fun start() {
        require(!waitingForMorePlayers) { "not enough players to start the game - ${players.size}/$roomSizeToStartGame" }

        _state = GameState.InProgress
        players.forEach { hands[it] = mutableListOf() }
        recordEvent(GameEvent.GameStarted(players))
        startNextRound()
    }

    private fun dealCards() {
        val deck = Deck.new()
        hands.replaceAll { playerId, _ ->
            riggedHands?.get(playerId)?.toMutableList() ?: deck.takeCards(roundNumber).toMutableList()
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
        if (phase != Bidding) throw GameException.CannotBid("not in bidding phase")
        if (bid < 0 || bid > roundNumber) throw GameException.CannotBid("bid $bid is greater than the round number ($roundNumber)")
        if (_bids.hasPlayerAlreadyBid(playerId)) throw GameException.CannotBid("player $playerId has already bid")

        _bids.place(playerId, bid)
        recordEvent(GameEvent.BidPlaced(playerId, Bid(bid)))

        if (_bids.areComplete) {
            this._phase = BiddingCompleted
            recordEvent(GameEvent.BiddingCompleted(_bids.asCompleted()))
        }
    }

    fun playCard(playerId: PlayerId, cardName: CardName) {
        if (phase != TrickTaking) throw GameException.CannotPlayCard("not in trick taking phase - phase is $phase")
        if (currentPlayersTurn != playerId) throw GameException.CannotPlayCard("it is not $playerId's turn to play a card")

        val hand = getHandFor(playerId)
        val card = hand.find { it.name == cardName }
        requireNotNull(card) { "card $cardName not in $playerId's hand" }

        hand.remove(card)
        trick.add(PlayedCard(playerId, card))
        roundTurnOrder.removeFirst()

        recordEvent(GameEvent.CardPlayed(playerId, card))

        if (trick.isComplete) {
            _phase = TrickCompleted
            _trickWinner = trick.winner
            recordEvent(GameEvent.TrickCompleted(trick.winner))

            if (roundNumber == 10) {
                _state = GameState.Complete
                recordEvent(GameEvent.GameCompleted)
            }
        }
    }

    fun rigDeck(playerId: PlayerId, cards: List<Card>) {
        if (riggedHands == null) riggedHands = players.associateWith { emptyList<Card>() }.toMutableMap()
        riggedHands!![playerId] = cards
    }

    fun startNextRound() {
        _roundNumber += 1
        _trickNumber = 0

        _bids.initFor(players)
        _phase = Bidding
        roundTurnOrder = (1..roundNumber).flatMap { players }.toMutableList()
        dealCards()
        recordEvent(GameEvent.RoundStarted(roundNumber))
    }

    fun startNextTrick() {
        _trickNumber += 1
        trick = Trick(players.size)
        _phase = TrickTaking
        recordEvent(GameEvent.TrickStarted(trickNumber))
    }

    private fun recordEvent(event: GameEvent) {
        eventListeners.forEach { it.handle(event) }
    }
}

// TODO: introduce tiny types for these. e.g a small data class that represents the data
typealias PlayerId = String

enum class GameState {
    WaitingForMorePlayers,
    WaitingToStart,
    InProgress,
    Complete;

    companion object {
        private val mapper = values().associateBy { it.name }
        fun from(state: String) = mapper[state] ?: error("unknown state $state")
    }
}

enum class RoundPhase {
    Bidding,
    BiddingCompleted,
    TrickTaking,
    TrickCompleted;

    companion object {
        private val mapper = values().associateBy { it.name }
        fun from(phase: String) = mapper[phase] ?: error("unknown phase $phase")
    }
}

typealias Hand = List<Card>

data class PlayedCard(val playerId: PlayerId, val card: Card) {
    override fun toString(): String {
        return "${card.name} played by $playerId"
    }
}

data class Bid(val bid: Int)

private class Bids {
    private var bids = mutableMapOf<PlayerId, DeprecatedBid>()

    val areComplete get() = bids.none { it.value is DeprecatedBid.None }

    fun initFor(players: Collection<PlayerId>) {
        bids = players.associateWith { DeprecatedBid.None }.toMutableMap()
    }

    fun forDisplay(): Map<PlayerId, DeprecatedBid> = when {
        areComplete -> bids
        else -> bids.mapValues { if (it.value is DeprecatedBid.Placed) DeprecatedBid.IsHidden else it.value }
    }

    fun place(playerId: PlayerId, bid: Int) {
        bids[playerId] = DeprecatedBid.Placed(bid)
    }

    fun hasPlayerAlreadyBid(playerId: PlayerId): Boolean {
        return bids[playerId] !is DeprecatedBid.None
    }

    fun asCompleted(): Map<PlayerId, Int> {
        return bids.mapValues {
            require(it.value !is DeprecatedBid.None)
            when (val bid = it.value) {
                is DeprecatedBid.None -> error("not all players have bid")
                is DeprecatedBid.IsHidden -> error("this should be impossible. this is just for display")
                is DeprecatedBid.Placed -> bid.bid
            }
        }
    }

}

sealed class DeprecatedBid {
    override fun toString(): String = when (this) {
        is None -> "None"
        is IsHidden -> "Hidden"
        is Placed -> bid.toString()
    }

    object None : DeprecatedBid()
    object IsHidden : DeprecatedBid()

    data class Placed(val bid: Int) : DeprecatedBid()
}
