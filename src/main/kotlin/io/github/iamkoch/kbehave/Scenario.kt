package io.github.iamkoch.kbehave

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Marks a method as a KBehave scenario.
 *
 * A scenario is a BDD-style test that consists of multiple steps,
 * each described with natural language.
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
@TestTemplate
@ExtendWith(ScenarioExtension::class)
annotation class Scenario(
    val displayName: String = ""
)
