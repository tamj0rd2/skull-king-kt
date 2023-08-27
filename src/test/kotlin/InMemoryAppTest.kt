import com.tamj0rd2.domain.App
import testsupport.ParticipateInGames
import testsupport.ManageGames
import testsupport.SkipWip
import testsupport.adapters.DomainDriver

class InMemoryAppTest : AppTestContract(object : TestConfiguration {
    override fun setup() {}

    override fun teardown() {}

    private val app = App()

    override fun participateInGames(): ParticipateInGames = ParticipateInGames(DomainDriver(app))

    override fun manageGames(): ManageGames = ManageGames(DomainDriver(app))
})
