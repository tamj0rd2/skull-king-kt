import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import testsupport.annotations.SkipUnhappyPathTests
import testsupport.annotations.SkipWipTests

@SkipWipTests
@SkipUnhappyPathTests
// NOTE: change this to ExecutionMode.SAME_THREAD if you want to debug tests. The output is much more useful.
@Execution(ExecutionMode.SAME_THREAD)
@DisabledIfEnvironmentVariable(named = "QUICK", matches = "true")
class SvelteBrowserAppTest : AppTestContract(BrowserTestConfiguration(useSvelte = true))
