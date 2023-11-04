package testsupport.annotations

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils
import kotlin.reflect.KClass

/**
 * This class is used to skip tests that have both a class level annotation and a test level annotation.
 * The tests will only be skipped if the test class has the class level annotation, and the executed test method
 * has the test level annotation.
 *
 * @property classLevelAnnotation the class level annotation that will allow tests to be skipped
 * @property testLevelAnnotation the test level annotation that will cause tests to be skipped when the [classLevelAnnotation] is present
 *
 * @sample WipExtension
 */
sealed class SkipCondition<C : Annotation, T : Annotation>(
    private val classLevelAnnotation: KClass<C>,
    private val testLevelAnnotation: KClass<T>
) : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val canSkipTests = context.hasTestClassAnnotation(classLevelAnnotation.java)
        val shouldSkipTest = AnnotationUtils.findAnnotation(context.element, testLevelAnnotation.java).isPresent

        return if (canSkipTests && shouldSkipTest) ConditionEvaluationResult.disabled("This test has the ${classLevelAnnotation.simpleName} and ${testLevelAnnotation.simpleName} annotations")
        else ConditionEvaluationResult.enabled("This test is missing the ${classLevelAnnotation.simpleName} or ${testLevelAnnotation.simpleName} annotations")
    }
}
