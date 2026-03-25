package crocalert.app

/**
 * Test classification annotations for selective CI execution (P3).
 *
 * Usage on test classes or individual test functions:
 *
 *   @UnitTest
 *   class AlertsViewModelTest { ... }
 *
 *   @IntegrationTest
 *   class AlertsRouteTest { ... }
 *
 * Gradle selective execution (exclude slow tests on PR builds):
 *   tasks.withType<Test> {
 *       useJUnitPlatform { excludeTags("slow") }
 *   }
 *
 * Note: Annotations are defined in each module's test sources so they are
 * available without cross-module test dependencies.
 */

/** Fast, isolated test — no I/O, no coroutine dispatch, no framework. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UnitTest

/** Tests that cross a module or service boundary (e.g. ViewModel + Repository). */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegrationTest

/** Full user-journey or Compose UI test — requires emulator or device. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class E2ETest

/** Test that verifies a specific bug regression — never delete without a link to the issue. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegressionTest

/** Test that exercises a performance-sensitive path or takes > 500 ms. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlowTest
