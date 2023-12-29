package com.tamj0rd2.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.tamj0rd2.domain.Card.NumberedCard
import com.tamj0rd2.domain.GameErrorCode.*
import com.tamj0rd2.domain.RoundPhase.*
import kotlinx.serialization.Serializable

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

    var trickNumber: TrickNumber = TrickNumber.None
        private set

    var roundNumber: RoundNumber = RoundNumber.None
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

    fun perform(command: GameMasterCommand) = when (command) {
        is GameMasterCommand.RigDeck -> rigDeck(command.playerId, command.cards)
        is GameMasterCommand.StartGame -> start()
        is GameMasterCommand.StartNextRound -> startNextRound()
        is GameMasterCommand.StartNextTrick -> startNextTrick()
    }

    fun perform(command: PlayerCommand): Result<Unit, CommandError> = when (command) {
        is PlayerCommand.JoinGame -> addPlayer(command.actor)
        is PlayerCommand.PlaceBid -> bid(command.actor, command.bid)
        is PlayerCommand.PlayCard -> playCard(command.actor, command.cardName)
    }

    private fun addPlayer(playerId: PlayerId) = playerCommand(playerId) {
        if (_players.contains(playerId)) PlayerWithSameNameAlreadyInGame.throwException()

        _players += playerId
        if (!waitingForMorePlayers) state = GameState.WaitingToStart
        recordEvent(GameEvent.PlayerJoined(playerId))

        Ok(Unit)
    }

    private fun start() = gameMasterCommand {
        require(!waitingForMorePlayers) { "not enough players to start the game - ${players.size}/$roomSizeToStartGame" }

        state = GameState.InProgress
        players.forEach { hands[it] = mutableListOf() }
        recordEvent(GameEvent.GameStarted(players))
    }

    private fun bid(playerId: PlayerId, bid: Bid) = playerCommand(playerId) {
        when {
            state != GameState.InProgress -> GameNotInProgress
            phase != Bidding -> BiddingIsNotInProgress
            // TODO: this could do with some love
            bid.value < 0 || bid.value > roundNumber.value -> BidLessThan0OrGreaterThanRoundNumber
            _bids.hasPlayerAlreadyBid(playerId) -> AlreadyPlacedABid
            else -> null
        }?.throwException()

        _bids.place(playerId, bid)
        recordEvent(GameEvent.BidPlaced(playerId, bid))

        if (_bids.areComplete) {
            this.phase = BiddingCompleted
            recordEvent(GameEvent.BiddingCompleted(_bids.asCompleted()))
        }

        Ok(Unit)
    }

    private fun playCard(playerId: PlayerId, cardName: CardName) = playerCommand(playerId) {
        when {
            phase != TrickTaking -> TrickNotInProgress
            currentPlayersTurn != playerId -> NotYourTurn
            else -> null
        }?.throwException()

        val suitBeforePlayingCard = trick.suit
        val hand = hands[playerId] ?: error("player $playerId somehow doesn't have a hand")
        val card = hand.find { it.name == cardName }

        if (card == null) {
            return@playerCommand Err(CommandError.FailedToPlayCard(
                playerId = playerId,
                cardName = cardName,
                reason = CardNotInHand,
                trick = currentTrick,
                hand = hand
            ))
        }

        if (!trick.isCardPlayable(card, hand.excluding(card))) {
            return@playerCommand Err(CommandError.FailedToPlayCard(
                playerId = playerId,
                cardName = cardName,
                reason = PlayingCardWouldBreakSuitRules,
                trick = currentTrick,
                hand = hand
            ))
        }

        hand.remove(card)
        trick.add(PlayedCard(playerId, card))

        roundTurnOrder.removeFirst()
        recordEvent(GameEvent.CardPlayed(playerId, card))

        if (suitBeforePlayingCard == null && card is NumberedCard) {
            recordEvent(GameEvent.SuitEstablished(card.suit))
        }

        if (trick.isComplete) {
            phase = TrickCompleted
            trickWinner = trick.winner
            _winsOfTheRound[trickWinner!!] = _winsOfTheRound[trickWinner!!]!! + 1
            recordEvent(GameEvent.TrickCompleted(trick.winner))

            if (isLastRound) {
                state = GameState.Complete
                recordEvent(GameEvent.GameCompleted)
            }
        }

        Ok(Unit)
    }

    private fun rigDeck(playerId: PlayerId, cards: List<Card>) = gameMasterCommand {
        if (riggedHands == null) riggedHands = players.associateWith { emptyList<Card>() }.toMutableMap()
        riggedHands!![playerId] = cards
    }

    private fun startNextRound() = gameMasterCommand {
        roundNumber += 1
        trickNumber = TrickNumber.of(0)

        _bids.initFor(players)
        phase = Bidding
        roundTurnOrder = (1..roundNumber.value).flatMap { players }.toMutableList()
        players.forEach { _winsOfTheRound[it] = 0 }

        dealCards()
        recordEvent(GameEvent.RoundStarted(roundNumber))
    }

    private fun startNextTrick() = gameMasterCommand {
        trickNumber += 1
        trick = Trick(players.size)
        phase = TrickTaking
        recordEvent(GameEvent.TrickStarted(trickNumber, roundTurnOrder.take(players.size)))
    }

    fun getCardsInHand(playerId: PlayerId): List<CardWithPlayability>? {
        val hand = hands[playerId]
        return hand?.map { card -> CardWithPlayability(card, trick.isCardPlayable(card, hand.excluding(card))) }
    }

    private fun dealCards() {
        val deck = Deck.new()
        hands.replaceAll { playerId, _ ->
            riggedHands?.get(playerId)?.toMutableList() ?: deck.takeCards(roundNumber.value).toMutableList()
        }
        recordEvent(GameEvent.CardsDealt(hands))
    }

    private val eventListeners = mutableListOf<GameEventListener>()

    private fun gameMasterCommand(block: () -> Unit) = command(null) { block(); Ok(Unit) }
    private fun playerCommand(playerId: PlayerId, block: () -> Result<Unit, CommandError>): Result<Unit, CommandError> {
        return command(playerId, block)
    }

    private fun command(triggeredBy: PlayerId?, block: () -> Result<Unit, CommandError>): Result<Unit, CommandError> {
        return block().andThen {
            val events = eventsBuffer.toList()
            eventsBuffer.clear()
            _allEventsSoFar.addAll(events)
            eventListeners.forEach { listener -> listener.handle(events, triggeredBy) }
            Ok(Unit)
        }
    }

    private val eventsBuffer = mutableListOf<GameEvent>()
    val allEventsSoFar get(): List<GameEvent> = _allEventsSoFar
    private val _allEventsSoFar = mutableListOf<GameEvent>()

    private fun recordEvent(event: GameEvent) {
        eventsBuffer.add(event)
    }

    private val isLastRound get() = roundNumber.value == 10
}

private fun <E> List<E>.excluding(element: E): List<E> {
    return filter { it != element }
}

@Serializable
enum class GameState {
    WaitingForMorePlayers,
    WaitingToStart,
    InProgress,
    Complete;

    companion object {
        private val mapper = entries.associateBy { it.name }
        fun from(state: String) = mapper[state] ?: error("unknown state $state")
    }
}

@Serializable
enum class RoundPhase {
    Bidding,
    BiddingCompleted,
    TrickTaking,
    TrickCompleted;

    companion object {
        private val mapper = entries.associateBy { it.name }
        fun from(phase: String) = mapper[phase] ?: error("unknown phase $phase")
    }
}

typealias Hand = List<Card>

@Serializable
data class PlayedCard(val playerId: PlayerId, val card: Card) {
    override fun toString(): String {
        return "${card.name} played by $playerId"
    }
}

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

    fun place(playerId: PlayerId, bid: Bid) {
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
                is DisplayBid.Placed -> bid.bid
            }
        }
    }
}

@Serializable
sealed class DisplayBid {
    override fun toString(): String = when (this) {
        is None -> "None"
        is Hidden -> "Hidden"
        is Placed -> bid.toString()
    }

    @Serializable
    object None : DisplayBid()

    @Serializable
    object Hidden : DisplayBid()

    @Serializable
    data class Placed(val bid: Bid) : DisplayBid()
}
