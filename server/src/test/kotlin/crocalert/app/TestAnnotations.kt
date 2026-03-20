package crocalert.app

/** Fast, isolated test — no I/O, no framework. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UnitTest

/** Tests that cross a module or service boundary (e.g. Ktor route + service). */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntegrationTest

/** Test that verifies a specific bug regression. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegressionTest

/** Test that exercises a performance-sensitive path or takes > 500 ms. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlowTest
