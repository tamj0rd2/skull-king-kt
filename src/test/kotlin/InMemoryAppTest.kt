import testsupport.ApplicationDriver
import testsupport.adapters.DomainDriver

class InMemoryAppTest : AppTestContract() {
    private val app = App()

    override val makeDriver = { DomainDriver(app) }
}