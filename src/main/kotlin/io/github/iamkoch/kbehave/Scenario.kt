package io.github.iamkoch.kbehave

/**
 * Marks a method as a KBehave scenario.
 *
 * A scenario is a BDD-style test that consists of multiple steps,
 * each described with natural language. The KBehave TestEngine discovers
 * methods annotated with `@Scenario` and executes them, reporting each
 * step as a separate test node.
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
annotation class Scenario(
    val displayName: String = ""
)
