
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.Game
import com.tamj0rd2.domain.GameEvent
import com.tamj0rd2.domain.GameMasterCommand
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
import testsupport.GameMasterDriver
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.DomainDriver
import testsupport.annotations.DoesNotSupportAutomatedGameMaster
import kotlin.time.Duration

class InMemoryTestConfiguration : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    override var automaticGameMasterDelay: Duration = Duration.ZERO

    private val game by lazy { Game() }

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(DomainDriver(game))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(game))
}

@Execution(ExecutionMode.SAME_THREAD)
@DoesNotSupportAutomatedGameMaster
class InMemoryAppTest : AppTestContract(InMemoryTestConfiguration())
