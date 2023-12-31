package com.tamj0rd2.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.tamj0rd2.domain.GameErrorCode.*
import com.tamj0rd2.domain.RoundPhase.*
import kotlinx.serialization.Serializable

fun interface GameEventListener {
    fun handle(events: List<GameEvent>, triggeredBy: PlayerId?)
}

data class ActualGameState(
    val trickNumber: TrickNumber,
    val roundNumber: RoundNumber,
    val state: GamePhase,
    val phase: RoundPhase?,
    val players: List<PlayerId>,
    val hands: Map<PlayerId, List<Card>>,
    val riggedHands: Map<PlayerId, List<Card>>,
    val bids: Map<PlayerId, Bid>,
    val trick: Trick?,
    val roundTurnOrder: List<PlayerId>,
)

class Game {
    private val currentPlayersTurn get(): PlayerId? = roundTurnOrder.firstOrNull()

    var trickNumber: TrickNumber = TrickNumber.None
        private set

    var roundNumber: RoundNumber = RoundNumber.None
        private set

    var state: GamePhase = GamePhase.WaitingForMorePlayers
        private set

    private var phase: RoundPhase? = null
    private val players = mutableListOf<PlayerId>()
    private val hands = mutableMapOf<PlayerId, MutableList<Card>>()
    private var riggedHands: MutableMap<PlayerId, List<Card>>? = null
    private val bids = mutableMapOf<PlayerId, Bid>()
    private val roomSizeToStartGame = 2
    private val waitingForMorePlayers get() = players.size < roomSizeToStartGame
    private var trick: Trick? = null
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
        if (players.contains(playerId)) PlayerWithSameNameAlreadyInGame.throwException()

        players += playerId
        if (!waitingForMorePlayers) state = GamePhase.WaitingToStart
        recordEvent(GameEvent.PlayerJoined(playerId))

        Ok(Unit)
    }

    private fun start() = gameMasterCommand {
        require(!waitingForMorePlayers) { "not enough players to start the game - ${players.size}/$roomSizeToStartGame" }

        state = GamePhase.InProgress
        players.forEach { hands[it] = mutableListOf() }
        recordEvent(GameEvent.GameStarted(players))
    }

    private fun bid(playerId: PlayerId, bid: Bid) = playerCommand(playerId) {
        when {
            state != GamePhase.InProgress -> GameNotInProgress
            phase != Bidding -> BiddingIsNotInProgress
            // TODO: this could do with some love
            bid.value < 0 || bid.value > roundNumber.value -> BidLessThan0OrGreaterThanRoundNumber
            bids.contains(playerId) -> AlreadyPlacedABid
            else -> null
        }?.throwException()

        bids[playerId] = bid
        recordEvent(GameEvent.BidPlaced(playerId, bid))

        if (bids.size == players.size) {
            this.phase = BiddingCompleted
            recordEvent(GameEvent.BiddingCompleted(bids))
        }

        Ok(Unit)
    }

    private fun playCard(playerId: PlayerId, cardName: CardName) = playerCommand(playerId) {
        when {
            phase != TrickTaking -> TrickNotInProgress
            currentPlayersTurn != playerId -> NotYourTurn
            else -> null
        }?.throwException()

        var trick = requireNotNull(trick) { "trick is null" }
        val hand = hands[playerId] ?: error("player $playerId somehow doesn't have a hand")
        val card = hand.find { it.name == cardName }

        if (card == null) {
            return@playerCommand Err(
                CommandError.FailedToPlayCard(
                    playerId = playerId,
                    cardName = cardName,
                    reason = CardNotInHand,
                    trick = requireNotNull(trick) { "trick is null" }.playedCards,
                    hand = hand
                )
            )
        }

        if (!trick.isCardPlayable(card, hand.excluding(card))) {
            return@playerCommand Err(
                CommandError.FailedToPlayCard(
                    playerId = playerId,
                    cardName = cardName,
                    reason = PlayingCardWouldBreakSuitRules,
                    trick = trick.playedCards,
                    hand = hand
                )
            )
        }

        hand.remove(card)
        trick = trick.add(PlayedCard(playerId, card))
        this.trick = trick

        roundTurnOrder.removeFirst()
        recordEvent(GameEvent.CardPlayed(playerId, card))

        if (trick.isComplete) {
            phase = TrickCompleted
            recordEvent(GameEvent.TrickCompleted(trick.winner))

            if (isLastRound) {
                state = GamePhase.Complete
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

        bids.clear()
        phase = Bidding
        roundTurnOrder = (1..roundNumber.value).flatMap { players }.toMutableList()

        dealCards()
        recordEvent(GameEvent.RoundStarted(roundNumber))
    }

    private fun startNextTrick() = gameMasterCommand {
        trickNumber += 1
        trick = Trick.ofSize(players.size)
        phase = TrickTaking
        recordEvent(GameEvent.TrickStarted(trickNumber, roundTurnOrder.take(players.size)))
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
enum class GamePhase {
    WaitingForMorePlayers,
    WaitingToStart,
    InProgress,
    Complete,
    ;

    companion object {
        private val mapperByName = entries.associateBy { it.name }
        fun from(state: String) = mapperByName[state] ?: error("unknown state $state")
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

@Serializable
data class PlayedCard(val playerId: PlayerId, val card: Card) {
    override fun toString(): String {
        return "${card.name} played by $playerId"
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

    companion object {
        fun parse(text: String): DisplayBid {
            return when (text) {
                "None" -> None
                "Hidden" -> Hidden
                else -> Placed(Bid.parse(text))
            }
        }
    }
}
