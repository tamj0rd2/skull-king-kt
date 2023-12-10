import com.tamj0rd2.webapp.Frontend
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests
import kotlin.test.Ignore

@Ignore
@SkipWipTests
@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
class SolidBrowserAppTest : AppTestContract(BrowserTestConfiguration(frontend = Frontend.Solid))
