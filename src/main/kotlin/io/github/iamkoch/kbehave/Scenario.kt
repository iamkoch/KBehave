package io.github.iamkoch.kbehave

import org.junit.jupiter.api.Test

/**
 * Marks a method as a KBehave scenario.
 *
 * A scenario is a BDD-style test that consists of multiple steps,
 * each described with natural language.
 *
 * Each step is reported as a separate test node in the test runner,
 * allowing you to see exactly which step passed or failed.
 *
 * Example:
 * ```kotlin
 * @Scenario
 * fun addition() {
 *     var result = 0
 *     "Given a calculator" x { }
 *     "When I add 2 and 3" x { result = 2 + 3 }
 *     "Then the result should be 5" x { assertEquals(5, result) }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Test
annotation class Scenario(
    val displayName: String = ""
)
