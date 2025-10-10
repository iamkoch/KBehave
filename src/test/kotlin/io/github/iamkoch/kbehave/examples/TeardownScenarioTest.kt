package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.step
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Examples demonstrating teardown functionality.
 */
class TeardownScenarioTest {

    class Resource(val name: String) {
        var isOpen = false
        var isClosed = false

        fun open() {
            isOpen = true
            println("  → Resource '$name' opened")
        }

        fun close() {
            isClosed = true
            isOpen = false
            println("  → Resource '$name' closed")
        }
    }

    @Scenario
    fun `scenario with resource cleanup`() {
        var resource: Resource? = null

        "Given a resource" x {
            resource = Resource("test-resource")
            resource!!.open()
            assertTrue(resource!!.isOpen, "Resource should be open during step execution")
        } teardown {
            resource?.close()
            println("  → Teardown executed")
        }

        "When I check the resource was cleaned up" x {
            assertTrue(resource!!.isClosed, "Resource should be closed by teardown")
        }

        "Then subsequent steps continue normally" x {
            // Test continues successfully
        }
    }

    @Scenario
    fun `scenario with multiple teardowns`() {
        val resources = mutableListOf<Resource>()

        "Given multiple resources" x {
            resources.add(Resource("resource-1").apply { open() })
            resources.add(Resource("resource-2").apply { open() })
            resources.add(Resource("resource-3").apply { open() })
            assertTrue(resources.all { it.isOpen }, "All resources should be open during step execution")
        } teardown {
            resources.forEach { it.close() }
        }

        "When I verify they were all cleaned up" x {
            assertTrue(resources.all { it.isClosed }, "All resources should be closed by teardown")
        }

        "Then the test continues successfully" x {
            // Test continues
        }
    }
}
