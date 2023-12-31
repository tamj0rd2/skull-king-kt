import com.tamj0rd2.webapp.Frontend
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests

@SkipWipTests
@SkipUnhappyPathTests
@Execution(ExecutionMode.SAME_THREAD)
class SvelteBrowserAppTest : AppTestContract(BrowserTestConfiguration(frontend = Frontend.Svelte))
