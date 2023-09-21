
import com.tamj0rd2.domain.Game
import testsupport.ManageGames
import testsupport.ParticipateInGames
import testsupport.adapters.DomainDriver

class InMemoryAppTest : AppTestContract(object : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    private val game = Game()

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(DomainDriver(game))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(game))
})
