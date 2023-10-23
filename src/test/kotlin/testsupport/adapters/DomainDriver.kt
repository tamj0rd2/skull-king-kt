package testsupport.adapters

import com.tamj0rd2.domain.Command
import com.tamj0rd2.domain.Command.PlayerCommand
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(private val game: Game) : ApplicationDriver, GameMasterDriver {

    private lateinit var playerId: PlayerId

    override fun perform(command: PlayerCommand) {
        if (command is PlayerCommand.JoinGame) playerId = command.actor
        game.perform(command)
    }

    override fun perform(command: Command.GameMasterCommand) {
        game.perform(command)
    }

    override val winsOfTheRound: Map<PlayerId, Int>
        get() = game.winsOfTheRound

    override val trickWinner: PlayerId? get() = game.trickWinner

    override val playersInRoom get() = game.players
    override val hand get() = game.getCardsInHand(playerId)
    override val trick: List<PlayedCard> get() = game.currentTrick.playedCards
    override val gameState: GameState get() = game.state
    override val roundPhase: RoundPhase get() = game.phase
    override val bids: Map<PlayerId, DisplayBid> get() = game.bids
    override val trickNumber: Int get() = game.trickNumber
    override val roundNumber: Int get() = game.roundNumber
    override val currentPlayer: PlayerId? get() = game.currentPlayersTurn
}