package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.CustomJackson.auto
import com.tamj0rd2.webapp.MessageFromClient
import com.tamj0rd2.webapp.MessageId
import com.tamj0rd2.webapp.MessageToClient
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testsupport.ApplicationDriver
import testsupport.retry
import java.time.Instant.now
import kotlin.time.Duration.Companion.seconds

class WsDriver(private val makeWs: (PlayerId) -> Websocket) : ApplicationDriver {
    private val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
    private val messageFromClientLens = WsMessage.auto<MessageFromClient>().toLens()

    private lateinit var logger: Logger
    private lateinit var ws: Websocket
    private lateinit var playerId: PlayerId
    private var playableCards: MutableMap<CardName, Boolean> = mutableMapOf()

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("wsClient:$playerId")
        this.playerId = playerId

        ws = makeWs(playerId)
        ws.onClose { logger.warn("websocket closed") }
        ws.onError { logger.error("websocket error", it) }
        ws.onMessage {
            synchronized(syncObject) {
                val message = messageToClientLens(it)
                logger.info("received: $message")

                handleIncomingMessage(message)
                if (message.needsAck) sendMessage(message.ack())
            }
        }

        sendMessage(MessageFromClient.JoinGame())
    }

    override fun bid(bid: Int) {
        logger.info("bidding $bid")
        if (roundPhase != RoundPhase.Bidding)
            throw GameException.CannotBid("not in ${RoundPhase.Bidding} phase. currently in the $roundPhase phase")

        val currentRoundNumber = roundNumber ?: error("$playerId cannot bid because round number is null")
        if (bid > currentRoundNumber)
            throw GameException.CannotBid("bid $bid is greater than the round number $currentRoundNumber")

        val currentBid = this.bids[playerId]
        requireNotNull(currentBid) { "$playerId is not in the bidding map" }

        if (currentBid != DisplayBid.None)
            throw GameException.CannotBid("$playerId has already bid")

        sendMessage(MessageFromClient.BidPlaced(bid))
    }

    override fun playCard(card: Card) {
        if (roundPhase != RoundPhase.TrickTaking) throw GameException.CannotPlayCard("currently in the $roundPhase phase")
        sendMessage(MessageFromClient.CardPlayed(card.name))
    }

    override fun isCardPlayable(card: Card): Boolean {
        if (playableCards.isEmpty()) throw GameException.CannotPlayCard("no cards are playable")
        if (currentPlayer != playerId) throw GameException.CannotPlayCard("it is $currentPlayer's turn")
        return playableCards[card.name] ?: error("card $card not in $playerId's hand")
    }

    override var winsOfTheRound: Map<PlayerId, Int> = emptyMap()

    override var trickWinner: PlayerId? = null

    override var currentPlayer: PlayerId? = null

    override var trickNumber: Int? = null

    override var roundNumber: Int? = null

    override val trick: MutableList<PlayedCard> = mutableListOf()

    override var roundPhase: RoundPhase? = null

    override var gameState: GameState? = null

    override var playersInRoom: List<PlayerId> = emptyList()

    override var hand: MutableList<Card> = mutableListOf()

    override var bids: MutableMap<PlayerId, DisplayBid> = mutableMapOf()

    private val messageAcknowledgements = mutableMapOf<MessageId, Boolean>()

    private val syncObject = Object()

    private fun sendMessage(message: MessageFromClient) {
        logger.info("sending: $message")
        retry(
            1.seconds,
            100,
            listOf(WebsocketNotConnectedException::class)
        ) { ws.send(messageFromClientLens(message)) }

        if (!message.needsAck) return

        messageAcknowledgements[message.id] = false

        synchronized(syncObject) {
            val mustFinishBy = now().plusMillis(250)

            do {
                if (messageAcknowledgements[message.id] == true) return
                syncObject.wait(50)
            } while (now() < mustFinishBy)

            error("message '${message::class.simpleName}' not acked by server")
        }
    }

    private fun handleIncomingMessage(message: MessageToClient) {
        when (message) {
            is MessageToClient.Ack -> {
                val messageId = message.acked.id
                require(messageAcknowledgements.contains(messageId)) { "message $messageId doesn't exist in acknowledgements map" }
                require(messageAcknowledgements[messageId] == false) { "message $messageId has already been acknowledged" }

                messageAcknowledgements[messageId] = true
                syncObject.notify()
            }

            is MessageToClient.BidPlaced -> {
                bids[message.playerId] = DisplayBid.Hidden
            }

            is MessageToClient.BiddingCompleted -> {
                bids = message.bids.map { it.key to DisplayBid.Placed(it.value.bid) }.toMap().toMutableMap()
                roundPhase = RoundPhase.BiddingCompleted
            }

            is MessageToClient.CardPlayed -> {
                if (message.playerId == playerId) {
                    hand.remove(message.card)
                }
                trick.add(message.card.playedBy(message.playerId))
                currentPlayer = message.nextPlayer
            }

            is MessageToClient.GameCompleted -> {
                gameState = GameState.Complete
            }

            is MessageToClient.GameStarted -> {
                playersInRoom = message.players
                gameState = GameState.InProgress
                roundPhase = RoundPhase.Bidding
            }

            is MessageToClient.Multi -> {
                message.messages.forEach(::handleIncomingMessage)
            }

            is MessageToClient.PlayerJoined -> {
                playersInRoom = message.players
                gameState = if (message.waitingForMorePlayers) {
                    GameState.WaitingForMorePlayers
                } else {
                    GameState.WaitingToStart
                }
            }

            is MessageToClient.RoundCompleted -> {
                winsOfTheRound = message.wins
            }

            is MessageToClient.RoundStarted -> {
                roundNumber = message.roundNumber
                bids = playersInRoom.associateWith { DisplayBid.None }.toMutableMap()
                hand = message.cardsDealt.toMutableList()
                roundPhase = RoundPhase.Bidding
                winsOfTheRound = emptyMap()
            }

            is MessageToClient.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
                trickWinner = message.winner
            }

            is MessageToClient.TrickStarted -> {
                roundPhase = RoundPhase.TrickTaking
                trickNumber = message.trickNumber
                currentPlayer = message.firstPlayer
                playableCards.clear()
                trick.clear()
            }

            is MessageToClient.YourTurn -> {
                playableCards = message.cards.toMutableMap()
            }
        }
    }
}

// TODO: get back here
//internal data class AckOrNack(val ack: MessageToClient.Ack?, val nack: MessageToClient.Nack?) {
//    val isAck = ack != null
//    val isNack = nack != null
//
//    init {
//        require(isAck xor isNack) { "AckOrNack must be either an Ack or a Nack" }
//    }
//}
