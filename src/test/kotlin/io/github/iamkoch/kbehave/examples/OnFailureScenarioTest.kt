package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.*
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(ScenarioExtension::class)
class OnFailureScenarioTest {

    @Scenario
    @TestTemplate
    fun `demonstration of onFailure RUN behavior`() {
        var setupRan = false
        var actionRan = false
        var cleanupRan = false

        "Given a resource is set up" x {
            setupRan = true
        } onFailure RemainingSteps.RUN

        "When an action is performed" x {
            actionRan = true
            // All previous steps should have executed
            assertEquals(true, setupRan)
        } onFailure RemainingSteps.RUN

        "Then cleanup happens" x {
            cleanupRan = true
            // All previous steps should have executed
            assertEquals(true, actionRan)
        } onFailure RemainingSteps.RUN
    }

    @Scenario
    @TestTemplate
    fun `demonstration of default SKIP behavior`() {
        var firstStepRan = false

        "Given first step runs" x {
            firstStepRan = true
        }

        "When second step runs" x {
            // First step should have executed
            assertEquals(true, firstStepRan)
        }
    }

    @Scenario
    @TestTemplate
    fun `using onFailure for cleanup steps`() {
        val resource = mutableListOf<String>()

        "Given a resource is created" x {
            resource.add("created")
        }

        "When the resource is used" x {
            resource.add("used")
            assertEquals(listOf("created", "used"), resource)
        } onFailure RemainingSteps.RUN

        "Then the resource is cleaned up" x {
            resource.add("cleaned")
            // Verify all operations completed
            assertEquals(listOf("created", "used", "cleaned"), resource)
        } onFailure RemainingSteps.RUN
    }
}
