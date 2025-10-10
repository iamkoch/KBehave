package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.Example
import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Examples demonstrating parameterized scenarios using @Example annotations.
 */
class ParameterizedScenarioTest {

    @Scenario
    @Example(intValues = [1, 2, 3])
    @Example(intValues = [2, 3, 5])
    @Example(intValues = [5, 7, 12])
    @Example(intValues = [10, 20, 30])
    fun `addition with multiple examples`(x: Int, y: Int, expected: Int) {
        var calculator: Calculator? = null
        var result = 0

        "Given a calculator" x {
            calculator = Calculator()
        }

        "And the numbers $x and $y" x {
            // Numbers provided via parameters
        }

        "When I add them together" x {
            result = calculator!!.add(x, y)
        }

        "Then the result should be $expected" x {
            assertEquals(expected, result)
        }
    }

    @Scenario
    @Example(intValues = [10, 5, 5])
    @Example(intValues = [20, 10, 10])
    @Example(intValues = [100, 50, 50])
    fun `subtraction with examples`(x: Int, y: Int, expected: Int) {
        var result = 0

        "Given the numbers $x and $y" x { }

        "When I subtract $y from $x" x {
            result = Calculator().subtract(x, y)
        }

        "Then the result should be $expected" x {
            assertEquals(expected, result)
        }
    }
}
