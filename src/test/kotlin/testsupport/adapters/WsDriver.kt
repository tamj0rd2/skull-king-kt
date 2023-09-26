package testsupport.adapters

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardName
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.CustomJackson.auto
import com.tamj0rd2.webapp.MessageFromClient
import com.tamj0rd2.webapp.MessageToClient
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testsupport.ApplicationDriver

class WsDriver(private val makeWs: (PlayerId) -> Websocket) : ApplicationDriver {
    private val messageToClientLens = WsMessage.auto<MessageToClient>().toLens()
    private val messageFromClientLens = WsMessage.auto<MessageFromClient>().toLens()

    private lateinit var logger: Logger
    private lateinit var ws: Websocket
    private lateinit var playerId: PlayerId
    private var playableCards: MutableMap<CardName, Boolean> = mutableMapOf()

    private fun sendMessage(message: MessageFromClient) {
        Thread.sleep(10)
        logger.info("sending: $message")
        ws.send(messageFromClientLens(message))
    }

    override fun joinGame(playerId: PlayerId) {
        logger = LoggerFactory.getLogger("wsClient pid='$playerId'")
        this.playerId = playerId

        ws = makeWs(playerId)
        ws.onClose { logger.warn("websocket closed") }
        ws.onError { throw it }
        ws.onMessage {
            logger.info("received: ${it.bodyString()}")
            handleIncomingMessage(messageToClientLens(it))
        }
        sendMessage(MessageFromClient.Connected)
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
                playersInRoom = message.players
                gameState = if(message.waitingForMorePlayers) {
                    GameState.WaitingForMorePlayers
                } else {
                    GameState.WaitingToStart
                }
            }
            is MessageToClient.RoundCompleted -> {

            }
            is MessageToClient.RoundStarted -> {
                roundNumber = message.roundNumber
                hand = message.cardsDealt.toMutableList()
                roundPhase = RoundPhase.Bidding
            }
            is MessageToClient.TrickCompleted -> {
                roundPhase = RoundPhase.TrickCompleted
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

    override fun bid(bid: Int) {
        sendMessage(MessageFromClient.BidPlaced(bid))
    }

    override fun playCard(card: Card) {
        sendMessage(MessageFromClient.CardPlayed(card.name))
    }

    override fun isCardPlayable(card: Card): Boolean {
        return playableCards[card.name] ?: error("card $card not in $playerId's hand")
    }

    override val winsOfTheRound: Map<PlayerId, Int> = emptyMap()

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
}