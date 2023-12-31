package testsupport.annotations

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.ModifierSupport
import org.junit.platform.commons.util.AnnotationUtils

internal fun <T : Annotation> ExtensionContext.hasTestClassAnnotation(wantedAnnotation: Class<T>) =
    findTestClassAnnotation(wantedAnnotation) != null

internal fun <T : Annotation> ExtensionContext.findTestClassAnnotation(wantedAnnotation: Class<T>): T? {
    val annotation = AnnotationUtils.findAnnotation(testClass, wantedAnnotation)
    if (annotation.isPresent) return annotation.get()

    val parent = parent.orElse(null)

    if (parent != null && parent != root) {
        return parent.findTestClassAnnotation(wantedAnnotation)
    }

    return null
}

internal fun <T, A : Annotation> Class<T>.isAnnotatedWith(wantedAnnotation: Class<A>): Boolean =
    AnnotationSupport.isAnnotated(this, wantedAnnotation)

internal val <T> Class<T>.isInnerClass: Boolean get() = ModifierSupport.isNotStatic(this) && isMemberClass
