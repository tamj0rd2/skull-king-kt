
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerGameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.ApplicationDriver
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.DomainDriver
import testsupport.annotations.DoesNotSupportAutomatedGameMaster
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests
import kotlin.time.Duration

@SkipWipTests
@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
@DoesNotSupportAutomatedGameMaster
class ExperimentalInMemoryAppTest : AppTestContract(ExperimentalInMemoryTestConfiguration())

class ExperimentalInMemoryTestConfiguration : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    override var automaticGameMasterDelay: Duration = Duration.ZERO

    private val game by lazy { Game() }

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(ExperimentalDriver(game))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(game))
}

private class ExperimentalDriver(private val game: Game) : ApplicationDriver {
    private lateinit var playerGameState: PlayerGameState

    override fun perform(command: PlayerCommand) {
        if (command is PlayerCommand.JoinGame && !this::playerGameState.isInitialized) {
            playerGameState = PlayerGameState.ofPlayer(command.actor, game.allEventsSoFar)
            game.subscribeToGameEvents { events, _ -> playerGameState = playerGameState.handle(events) }
        }

        return game.perform(command)
    }

    override val winsOfTheRound: Map<PlayerId, Int>
        get() = playerGameState.winsOfTheRound
    override val trickWinner: PlayerId?
        get() = playerGameState.trickWinner
    override val currentPlayer: PlayerId?
        get() = playerGameState.currentPlayer
    override val trickNumber: TrickNumber
        get() = playerGameState.trickNumber
    override val roundNumber: RoundNumber
        get() = playerGameState.roundNumber
    override val trick: List<PlayedCard>
        get() = playerGameState.trick
    override val roundPhase: RoundPhase?
        get() = playerGameState.roundPhase
    override val gameState: GameState?
        get() = playerGameState.gameState
    override val playersInRoom: List<PlayerId>
        get() = playerGameState.playersInRoom
    override val hand: List<CardWithPlayability>
        get() = playerGameState.hand
    override val bids: Map<PlayerId, DisplayBid>
        get() = playerGameState.bids
}
