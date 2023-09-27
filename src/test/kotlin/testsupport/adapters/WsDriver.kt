package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.CustomJackson.asCompactJsonString
import com.tamj0rd2.webapp.CustomJackson.asJsonObject
import com.tamj0rd2.webapp.CustomJackson.auto
import com.tamj0rd2.webapp.MessageFromClient
import com.tamj0rd2.webapp.MessageToClient
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testsupport.ApplicationDriver
import testsupport.retry
import kotlin.time.Duration.Companion.milliseconds

class WsDriver(private val makeWs: (PlayerId) -> Websocket) : ApplicationDriver {
    private val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
    private val messageFromClientLens = WsMessage.auto<MessageFromClient>().toLens()

    private lateinit var logger: Logger
    private lateinit var ws: Websocket
    private lateinit var playerId: PlayerId
    private var playableCards: MutableMap<CardName, Boolean> = mutableMapOf()
    private var hasJoinedGame: Boolean = false

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("wsClient:$playerId")
        this.playerId = playerId

        ws = makeWs(playerId)
        ws.onClose { logger.warn("websocket closed") }
        ws.onError { logger.error("websocket error", it) }
        ws.onMessage {
            logger.info("client received: ${it.bodyString()}")
            handleIncomingMessage(messageToClientLens(it))
        }

        // needed otherwise there can be a race condition between the server opening the socket and the client sending the connected message
        Thread.sleep(10)
        sendMessage(MessageFromClient.Connected)
        retry(500.milliseconds, backoffMs = 50) {
            // TODO: this is weird and difficult...
            if (!hasJoinedGame) {
                error("player $playerId hasn't joined the game yet")
            }
        }
    }

    override fun bid(bid: Int) {
        logger.info("bidding $bid")
        requireNotNull(roundNumber) { "round number is null" }

        if (bid > roundNumber!!) throw GameException.CannotBid("bid $bid is greater than the round number $roundNumber")

        val currentBid = this.bids[playerId]
        requireNotNull(currentBid) { "$playerId is not in the bidding map" }

        if (currentBid != DisplayBid.None) throw GameException.CannotBid("$playerId has already bid")
        sendMessage(MessageFromClient.BidPlaced(bid))
    }

    override fun playCard(card: Card) {
        if (roundPhase != RoundPhase.TrickTaking) throw GameException.CannotPlayCard("currently in the $roundPhase phase")
        sendMessage(MessageFromClient.CardPlayed(card.name))
    }

    override fun isCardPlayable(card: Card): Boolean {
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

    private fun sendMessage(message: MessageFromClient) {
        logger.info("sending: ${message.asJsonObject().asCompactJsonString()}")
        try {
            ws.send(messageFromClientLens(message))
        } catch (e: WebsocketNotConnectedException) {
            if (hasJoinedGame) {
                throw e
            }

            // TODO: this is weird and difficult...
            throw GameException.PlayerWithSameNameAlreadyJoined(playerId)
        }
    }

    private fun handleIncomingMessage(message: MessageToClient) {
        when(message) {
            is MessageToClient.BidPlaced -> bids[message.playerId] = DisplayBid.Hidden
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
            is MessageToClient.GameCompleted -> gameState = GameState.Complete
            is MessageToClient.GameStarted -> playersInRoom = message.players
            is MessageToClient.Multi -> message.messages.forEach(::handleIncomingMessage)
            is MessageToClient.PlayerJoined -> {
                if (message.playerId == playerId) {
                    hasJoinedGame = true
                }

                playersInRoom = message.players
                gameState = if(message.waitingForMorePlayers) {
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
            is MessageToClient.YourTurn -> playableCards = message.cards.toMutableMap()
        }
    }
}
