package io.github.iamkoch.kbehave.examples

import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Examples demonstrating inline teardown syntax.
 */
class InlineTeardownTest {

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
    fun `inline teardown syntax`() {
        var resource: Resource? = null

        "Given a resource" x {
            resource = Resource("test-resource")
            resource!!.open()
        } teardown {
            resource?.close()
            println("  → Teardown executed")
        }

        "When I check the resource was cleaned up" x {
            assertTrue(resource!!.isClosed, "Resource should be closed by teardown")
        }

        "Then the test continues" x {
            // Test continues successfully
        }
    }

    @Scenario
    fun `multiple inline teardowns`() {
        var resource1: Resource? = null
        var resource2: Resource? = null

        "Given the first resource" x {
            resource1 = Resource("resource-1")
            resource1!!.open()
        } teardown {
            resource1?.close()
        }

        "And the second resource" x {
            resource2 = Resource("resource-2")
            resource2!!.open()
        } teardown {
            resource2?.close()
        }

        "When I verify both are cleaned up" x {
            assertTrue(resource1!!.isClosed, "Resource 1 should be closed")
            assertTrue(resource2!!.isClosed, "Resource 2 should be closed")
        }
    }

    @Scenario
    fun `chained teardown calls`() {
        val cleanup1Done = mutableListOf<Boolean>()
        val cleanup2Done = mutableListOf<Boolean>()

        "Given a step with multiple teardowns" x {
            println("  → Executing step")
        } teardown {
            cleanup1Done.add(true)
            println("  → First teardown")
        } teardown {
            cleanup2Done.add(true)
            println("  → Second teardown")
        }

        "Then both teardowns should have executed" x {
            assertTrue(cleanup1Done.isNotEmpty(), "First teardown should have executed")
            assertTrue(cleanup2Done.isNotEmpty(), "Second teardown should have executed")
        }
    }
}
