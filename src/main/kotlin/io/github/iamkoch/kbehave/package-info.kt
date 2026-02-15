/**
 * KBehave — a Kotlin BDD testing framework that gives every step its own node in
 * the IDE test tree.
 *
 * ## Quick start
 *
 * ```kotlin
 * import io.github.iamkoch.kbehave.Scenario
 * import io.github.iamkoch.kbehave.x
 *
 * class CalculatorTest {
 *     @Scenario
 *     fun `simple addition`() {
 *         lateinit var calculator: Calculator
 *         var result = 0
 *
 *         "Given a calculator" x { calculator = Calculator() }
 *         "When I add 2 and 3" x { result = calculator.add(2, 3) }
 *         "Then the result should be 5" x { assertEquals(5, result) }
 *     }
 * }
 * ```
 *
 * ## Core API
 *
 * | Type | Purpose |
 * |------|---------|
 * | [Scenario] | Annotation that marks a method as a BDD scenario |
 * | [Example] | Repeatable annotation for parameterized scenario data |
 * | [x] | Infix function that defines a step: `"description" x { body }` |
 * | [StepDefinition] | Returned by [x]; supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining |
 * | [StepBuilder] | Builder obtained via [step]; supports skip/teardown before the step body |
 * | [RemainingSteps] | Enum controlling whether later steps run after a failure |
 *
 * ## BDD aliases
 *
 * For teams that prefer explicit keywords: [given], [when_], [then], [and], [but].
 * These are thin wrappers around [x].
 */
package io.github.iamkoch.kbehave
