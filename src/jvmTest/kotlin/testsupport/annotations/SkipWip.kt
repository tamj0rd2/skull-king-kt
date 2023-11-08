package testsupport.annotations

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Target(AnnotationTarget.CLASS)
annotation class SkipWipTests

@Test
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@ExtendWith(WipExtension::class)
annotation class Wip

class WipExtension : SkipCondition<SkipWipTests, Wip>(SkipWipTests::class, Wip::class)
