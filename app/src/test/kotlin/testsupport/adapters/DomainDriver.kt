package testsupport.adapters

import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(private val game: Game) : ApplicationDriver, GameMasterDriver {

    private var playerId = PlayerId.unidentified

    override fun perform(command: PlayerCommand) {
        if (command is PlayerCommand.JoinGame) playerId = command.actor
        game.perform(command)
    }

    override fun perform(command: GameMasterCommand) {
        game.perform(command)
    }

    override val winsOfTheRound: Map<PlayerId, Int>
        get() = game.winsOfTheRound

    override val trickWinner: PlayerId? get() = game.trickWinner

    override val playersInRoom get() = game.players
    override val hand get() = game.getCardsInHand(playerId) ?: error("$playerId has no hand at all")
    override val trick: List<PlayedCard> get() = game.currentTrick
    override val gameState: GameState get() = game.state
    override val roundPhase: RoundPhase? get() = game.phase
    override val bids: Map<PlayerId, DisplayBid> get() = game.bids
    override val trickNumber: TrickNumber get() = game.trickNumber
    override val roundNumber: RoundNumber get() = game.roundNumber
    override val currentPlayer: PlayerId? get() = game.currentPlayersTurn
}