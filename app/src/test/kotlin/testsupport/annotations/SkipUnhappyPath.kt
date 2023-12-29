package testsupport.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS)
annotation class SkipUnhappyPathTests

@Target(AnnotationTarget.FUNCTION)
@ExtendWith(UnhappyPathExtension::class)
annotation class UnhappyPath

class UnhappyPathExtension :
    SkipCondition<SkipUnhappyPathTests, UnhappyPath>(SkipUnhappyPathTests::class, UnhappyPath::class)
