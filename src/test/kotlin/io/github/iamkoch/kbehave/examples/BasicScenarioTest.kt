package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Basic scenario examples demonstrating the fundamental KBehave syntax.
 */
class BasicScenarioTest {

    @Scenario
    fun `simple_addition_scenario`() {
        var calculator: Calculator? = null
        var result = 0

        "Given a calculator" x {
            calculator = Calculator()
        }

        "When I add 2 and 3" x {
            result = calculator!!.add(2, 3)
        }

        "Then the result should be 5" x {
            assertEquals(5, result)
        }
    }

    @Scenario
    fun `scenario with string interpolation`() {
        val x = 10
        val y = 5
        var result = 0

        "Given the numbers $x and $y" x {
            // Numbers are already defined
        }

        "When I multiply them together" x {
            result = x * y
        }

        "Then the result should be ${x * y}" x {
            assertEquals(50, result)
        }
    }

    @Scenario
    fun `scenario with multiple operations`() {
        var calculator: Calculator? = null
        var sum = 0
        var product = 0

        "Given a calculator" x {
            calculator = Calculator()
        }

        "When I add 5 and 7" x {
            sum = calculator!!.add(5, 7)
        }

        "And I multiply 4 and 6" x {
            product = calculator!!.multiply(4, 6)
        }

        "Then the sum should be 12" x {
            assertEquals(12, sum)
        }

        "And the product should be 24" x {
            assertEquals(24, product)
        }
    }
}
