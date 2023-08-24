import com.tamj0rd2.domain.App
import testsupport.ParticipateInGames
import testsupport.ManageGames
import testsupport.adapters.DomainDriver

class InMemoryAppTest : AppTestContract() {
    private val app = App()

    override val participateInGames = { ParticipateInGames(DomainDriver(app)) }
    override val manageGames = { ManageGames(DomainDriver(app)) }
}