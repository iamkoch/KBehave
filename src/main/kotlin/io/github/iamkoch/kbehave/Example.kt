package io.github.iamkoch.kbehave

/**
 * Provides example data for parameterized scenarios.
 *
 * Similar to xBehave.net's [Example] attribute and Cucumber's Scenario Outlines,
 * this annotation allows you to run the same scenario with different input values.
 *
 * Example:
 * ```kotlin
 * @Scenario
 * @Example(intValues = [1, 2, 3])
 * @Example(intValues = [2, 3, 5])
 * @Example(intValues = [5, 7, 12])
 * fun addition(x: Int, y: Int, expected: Int) {
 *     var result = 0
 *     "Given the numbers $x and $y" x { }
 *     "When I add them together" x { result = x + y }
 *     "Then the result should be $expected" x { assertEquals(expected, result) }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Example(
    val intValues: IntArray = [],
    val longValues: LongArray = [],
    val stringValues: Array<String> = [],
    val booleanValues: BooleanArray = [],
    val doubleValues: DoubleArray = []
)
