import com.tamj0rd2.webapp.Frontend
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests

@Disabled("Disabled while I'm working on improving the backend")
@SkipWipTests
@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
class SolidBrowserAppTest : AppTestContract(BrowserTestConfiguration(frontend = Frontend.Solid))
