
import com.tamj0rd2.domain.Game
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.DomainDriver

@Execution(ExecutionMode.SAME_THREAD)
class InMemoryAppTest : AppTestContract(object : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    private val game = Game()

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(DomainDriver(game))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(game))
})
