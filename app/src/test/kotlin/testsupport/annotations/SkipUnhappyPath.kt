package testsupport.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS)
annotation class SkipUnhappyPathTests

@Test
@Target(AnnotationTarget.FUNCTION)
@ExtendWith(UnhappyPathExtension::class)
annotation class UnhappyPathTest

class UnhappyPathExtension :
    SkipCondition<SkipUnhappyPathTests, UnhappyPathTest>(SkipUnhappyPathTests::class, UnhappyPathTest::class)
