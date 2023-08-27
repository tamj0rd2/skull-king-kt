package testsupport

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation

@Target(AnnotationTarget.CLASS)
annotation class SkipWip

@Target(AnnotationTarget.FUNCTION)
@ExtendWith(WipExtension::class)
annotation class Wip

class WipExtension : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val skipWipAnnotation = findAnnotation(context.testClass, SkipWip::class.java)
        val wipAnnotation = findAnnotation(context.element, Wip::class.java)

        return if (skipWipAnnotation.isPresent && wipAnnotation.isPresent) ConditionEvaluationResult.disabled("This test has the WIP flag")
        else ConditionEvaluationResult.enabled("This test does not have the WIP flag")
    }
}
