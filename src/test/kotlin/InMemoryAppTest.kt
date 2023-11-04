
import com.tamj0rd2.domain.Game
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.DomainDriver
import testsupport.annotations.DoesNotSupportAutomatedGameMaster
import kotlin.time.Duration

@Execution(ExecutionMode.SAME_THREAD)
@DoesNotSupportAutomatedGameMaster
class InMemoryAppTest : AppTestContract(object : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    override var automaticGameMasterDelay: Duration = Duration.ZERO

    private val game by lazy { Game() }

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(DomainDriver(game))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(game))
})
