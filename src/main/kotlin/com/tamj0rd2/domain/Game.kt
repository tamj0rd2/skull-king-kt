package com.tamj0rd2.domain

import com.tamj0rd2.domain.RoundPhase.*

fun interface GameEventListener {
    fun handle(events: List<GameEvent>, triggeredBy: PlayerId?)
}

class Game {
    val winsOfTheRound: Map<PlayerId, Int> get() = _winsOfTheRound
    val currentTrick: List<PlayedCard> get() = trick.playedCards
    val players get() = _players.toList()
    val currentPlayersTurn get(): PlayerId? = roundTurnOrder.firstOrNull()
    val bids: Map<PlayerId, DisplayBid> get() = _bids.forDisplay()

    var trickWinner: PlayerId? = null
        private set

    var trickNumber: Int = 0
        private set

    var roundNumber: Int = 0
        private set

    var phase: RoundPhase? = null
        private set

    var state: GameState = GameState.WaitingForMorePlayers
        private set


    private val _winsOfTheRound = mutableMapOf<PlayerId, Int>()
    private val _players = mutableListOf<PlayerId>()
    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private var riggedHands: MutableMap<PlayerId, Hand>? = null
    private val _bids = Bids()
    private val roomSizeToStartGame = 2
    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame
    private var trick: Trick = Trick(0)
    private var roundTurnOrder = mutableListOf<PlayerId>()

    fun subscribeToGameEvents(listener: GameEventListener) {
        this.eventListeners += listener
    }

    fun isInState(state: GameState) = this.state == state

    fun perform(command: Command): List<GameEvent> = when(command) {
        is Command.GameMasterCommand.RigDeck -> rigDeck(command.playerId, command.cards)
        is Command.GameMasterCommand.StartGame -> start()
        is Command.GameMasterCommand.StartNextRound -> startNextRound()
        is Command.GameMasterCommand.StartNextTrick -> startNextTrick()
        is Command.PlayerCommand.JoinGame -> addPlayer(command.actor)
        is Command.PlayerCommand.PlaceBid -> bid(command.actor, command.bid.bid)
        is Command.PlayerCommand.PlayCard -> playCard(command.actor, command.cardName)
    }

    private fun addPlayer(playerId: PlayerId) = playerCommand(playerId) {
        if (_players.contains(playerId)) GameErrorCode.PlayerWithSameNameAlreadyInGame.throwException()

        _players += playerId
        if (!waitingForMorePlayers) state = GameState.WaitingToStart
        recordEvent(GameEvent.PlayerJoined(playerId))
    }

    private fun start() = gameMasterCommand {
        require(!waitingForMorePlayers) { "not enough players to start the game - ${players.size}/$roomSizeToStartGame" }

        state = GameState.InProgress
        players.forEach { hands[it] = mutableListOf() }
        recordEvent(GameEvent.GameStarted(players))
    }

    private fun bid(playerId: PlayerId, bid: Int) = playerCommand(playerId) {
        if (state != GameState.InProgress) throw GameException.CannotBid("game not in progress")
        if (phase != Bidding) throw GameException.CannotBid("not in bidding phase")
        if (bid < 0 || bid > roundNumber) throw GameException.CannotBid("bid $bid is greater than the round number ($roundNumber)")
        if (_bids.hasPlayerAlreadyBid(playerId)) throw GameException.CannotBid("player $playerId has already bid")

        _bids.place(playerId, bid)
        recordEvent(GameEvent.BidPlaced(playerId, Bid(bid)))

        if (_bids.areComplete) {
            this.phase = BiddingCompleted
            recordEvent(GameEvent.BiddingCompleted(_bids.asCompleted()))
        }
    }

    private fun playCard(playerId: PlayerId, cardName: CardName) = playerCommand(playerId) {
        if (phase != TrickTaking) throw GameException.CannotPlayCard("not in trick taking phase - phase is $phase")
        if (currentPlayersTurn != playerId) throw GameException.CannotPlayCard("it is not $playerId's turn to play a card")

        val hand = hands[playerId] ?: error("player $playerId somehow doesn't have a hand")
        val card = hand.find { it.name == cardName }
        requireNotNull(card) { "card $cardName not in $playerId's hand" }

        if (!trick.isCardPlayable(card, hand.excluding(card))) {
            throw GameException.CannotPlayCard("$card does not match suit of trick")
        }

        hand.remove(card)
        trick.add(PlayedCard(playerId, card))

        roundTurnOrder.removeFirst()
        recordEvent(GameEvent.CardPlayed(playerId, card))

        if (trick.isComplete) {
            phase = TrickCompleted
            trickWinner = trick.winner
            _winsOfTheRound[trickWinner!!] = _winsOfTheRound[trickWinner!!]!! + 1
            recordEvent(GameEvent.TrickCompleted(trick.winner))

            if (roundNumber == 10) {
                state = GameState.Complete
                recordEvent(GameEvent.GameCompleted)
            }
        }
    }

    private fun rigDeck(playerId: PlayerId, cards: List<Card>) = gameMasterCommand {
        if (riggedHands == null) riggedHands = players.associateWith { emptyList<Card>() }.toMutableMap()
        riggedHands!![playerId] = cards
    }

    private fun startNextRound() = gameMasterCommand {
        roundNumber += 1
        trickNumber = 0

        _bids.initFor(players)
        phase = Bidding
        roundTurnOrder = (1..roundNumber).flatMap { players }.toMutableList()
        players.forEach { _winsOfTheRound[it] = 0 }

        dealCards()
        recordEvent(GameEvent.RoundStarted(roundNumber))
    }

    private fun startNextTrick() = gameMasterCommand {
        trickNumber += 1
        trick = Trick(players.size)
        phase = TrickTaking
        recordEvent(GameEvent.TrickStarted(trickNumber))
    }

    fun getCardsInHand(playerId: PlayerId): List<CardWithPlayability> {
        val hand = hands[playerId]
        requireNotNull(hand) { "player $playerId somehow doesn't have a hand" }
        return hand.map { card -> CardWithPlayability(card, trick.isCardPlayable(card, hand.excluding(card))) }
    }

    private fun dealCards() {
        val deck = Deck.new()
        hands.replaceAll { playerId, _ ->
            riggedHands?.get(playerId)?.toMutableList() ?: deck.takeCards(roundNumber).toMutableList()
        }
    }

    private val eventListeners = mutableListOf<GameEventListener>()

    private fun gameMasterCommand(block: () -> Unit) = command(null, block)
    private fun playerCommand(playerId: PlayerId, block: () -> Unit) = command(playerId, block)

    private fun command(triggeredBy: PlayerId?, block: () -> Unit): List<GameEvent> {
        block()
        val events = eventsBuffer.toList()
        eventsBuffer.clear()
        eventListeners.forEach { listener -> listener.handle(events, triggeredBy) }
        return events
    }

    private val eventsBuffer = mutableListOf<GameEvent>()

    private fun recordEvent(event: GameEvent) {
        eventsBuffer.add(event)
    }
}

private fun <E> List<E>.excluding(element: E): List<E> {
    return filter { it != element }
}

data class PlayerId(val playerId: String) {
    override fun toString(): String = playerId

    companion object {
        val unidentified = PlayerId("unidentified")

        fun from(playerId: String) = PlayerId(playerId)
    }
}

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

// TODO: should make this a data class that gets copied...
private class Bids {
    private var bids = mutableMapOf<PlayerId, DisplayBid>()

    val areComplete get() = bids.none { it.value is DisplayBid.None }

    fun initFor(players: Collection<PlayerId>) {
        bids = players.associateWith { DisplayBid.None }.toMutableMap()
    }

    fun forDisplay(): Map<PlayerId, DisplayBid> = when {
        areComplete -> bids
        else -> bids.mapValues { if (it.value is DisplayBid.Placed) DisplayBid.Hidden else it.value }
    }

    fun place(playerId: PlayerId, bid: Int) {
        bids[playerId] = DisplayBid.Placed(bid)
    }

    fun hasPlayerAlreadyBid(playerId: PlayerId): Boolean {
        return bids[playerId] !is DisplayBid.None
    }

    fun asCompleted(): Map<PlayerId, Bid> {
        return bids.mapValues {
            require(it.value !is DisplayBid.None)
            when (val bid = it.value) {
                is DisplayBid.None -> error("not all players have bid")
                is DisplayBid.Hidden -> error("this should be impossible. this is just for display")
                is DisplayBid.Placed -> Bid(bid.bid)
            }
        }
    }
}

sealed class DisplayBid {
    override fun toString(): String = when (this) {
        is None -> "None"
        is Hidden -> "Hidden"
        is Placed -> bid.toString()
    }

    object None : DisplayBid()
    object Hidden : DisplayBid()

    data class Placed(val bid: Int) : DisplayBid()
}
