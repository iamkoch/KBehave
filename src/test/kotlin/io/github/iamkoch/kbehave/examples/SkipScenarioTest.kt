package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.step
import io.github.iamkoch.kbehave.x

/**
 * Examples demonstrating step skipping functionality.
 */
class SkipScenarioTest {

    @Scenario
    fun `scenario with skipped step`() {
        "Given this step executes" x {
            println("  → This executes")
        }

        "When this step is skipped".step()
            .skip("Feature not yet implemented") x {
            println("  → This should not execute")
        }

        "Then this step also executes" x {
            println("  → This also executes")
        }
    }

    @Scenario
    fun `scenario with conditional skip`() {
        val featureEnabled = false

        "Given a feature flag" x {
            println("  → Feature enabled: $featureEnabled")
        }

        val whenStep = "When the feature is used".step()
        if (!featureEnabled) {
            whenStep.skip("Feature disabled")
        }
        whenStep x {
            println("  → Using feature")
        }

        "Then the scenario continues" x {
            println("  → Scenario continues regardless")
        }
    }
}
