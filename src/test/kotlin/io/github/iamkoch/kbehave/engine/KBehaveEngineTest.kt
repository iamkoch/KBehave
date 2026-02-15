package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.Example
import io.github.iamkoch.kbehave.RemainingSteps
import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.EventConditions.*
import org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf

/**
 * Engine-level tests using junit-platform-testkit to verify KBehave engine behavior.
 */
class KBehaveEngineTest {

    // ---- Test fixtures (must be public for engine discovery) ----

    class BasicScenarioFixture {
        @Scenario
        fun `passing scenario`() {
            "Given a step" x { }
            "When another step" x { }
            "Then a final step" x { }
        }
    }

    class FailingStepFixture {
        @Scenario
        fun `failing middle step`() {
            "Given setup" x { }
            "When it fails" x { throw AssertionError("deliberate failure") }
            "Then this should be skipped" x { }
        }
    }

    class TeardownOnFailureFixture {
        companion object {
            var teardownExecuted = false
        }

        @Scenario
        fun `teardown runs on failure`() {
            teardownExecuted = false
            "Given a step with teardown" x {
                throw AssertionError("step fails")
            } teardown {
                teardownExecuted = true
            }
            "Then this is skipped" x { }
        }
    }

    class TeardownOnSuccessFixture {
        companion object {
            var teardownExecuted = false
        }

        @Scenario
        fun `teardown runs on success`() {
            teardownExecuted = false
            "Given a step with teardown" x {
                // succeeds
            } teardown {
                teardownExecuted = true
            }
            "Then this runs" x {
                assertTrue(teardownExecuted, "Teardown should have run after previous step")
            }
        }
    }

    class ParameterizedFixture {
        companion object {
            val executedValues = mutableListOf<Int>()
        }

        @Scenario
        @Example(intValues = [1, 2, 3])
        @Example(intValues = [4, 5, 9])
        fun `addition examples`(a: Int, b: Int, expected: Int) {
            "Given $a and $b" x { }
            "When I add them" x {
                val result = a + b
                assertEquals(expected, result)
                executedValues.add(expected)
            }
            "Then the result is $expected" x { }
        }
    }

    class EmptyScenarioFixture {
        @Scenario
        fun `empty scenario`() {
            // No steps
        }
    }

    class SingleStepFixture {
        @Scenario
        fun `single step`() {
            "The only step" x { }
        }
    }

    class OnFailureRunFixture {
        companion object {
            var thirdStepRan = false
        }

        @Scenario
        fun `continue after failure`() {
            thirdStepRan = false
            "Given setup" x { } onFailure RemainingSteps.RUN
            "When it fails" x {
                throw AssertionError("deliberate")
            } onFailure RemainingSteps.RUN
            "Then this still runs" x {
                thirdStepRan = true
            }
        }
    }

    class StepOrderingFixture {
        companion object {
            val executionOrder = mutableListOf<String>()
        }

        @Scenario
        fun `ordered steps`() {
            executionOrder.clear()
            "First" x { executionOrder.add("first") }
            "Second" x { executionOrder.add("second") }
            "Third" x { executionOrder.add("third") }
        }
    }

    class MultipleTeardownsFixture {
        companion object {
            val teardownOrder = mutableListOf<String>()
        }

        @Scenario
        fun `step with multiple teardowns`() {
            teardownOrder.clear()
            "Given a step" x {
                // succeeds
            } teardown {
                teardownOrder.add("teardown-1")
            } teardown {
                teardownOrder.add("teardown-2")
            }
            "Then check teardowns ran" x {
                assertEquals(listOf("teardown-1", "teardown-2"), teardownOrder)
            }
        }
    }

    class TeardownFailureSuppressedFixture {
        companion object {
            var bothRan = false
        }

        @Scenario
        fun `teardown failure is suppressed`() {
            bothRan = false
            "Given a failing step with failing teardown" x {
                throw AssertionError("step fails")
            } teardown {
                throw RuntimeException("teardown also fails")
            }
            "Then skipped" x { }
        }
    }

    // ---- Tests ----

    @Test
    fun `basic scenario discovers and executes all steps`() {
        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(BasicScenarioFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.started(3)
                stats.succeeded(3)
                stats.failed(0)
            }
    }

    @Test
    fun `failing step skips remaining steps`() {
        val events = EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(FailingStepFixture::class.java))
            .execute()
            .testEvents()

        // 3 steps: 1 passes, 1 fails, 1 aborted (skipped)
        events.assertStatistics { stats ->
            stats.started(3)
            stats.succeeded(1)
            stats.failed(1)
            stats.aborted(1)
        }
    }

    @Test
    fun `teardown executes when step fails`() {
        TeardownOnFailureFixture.teardownExecuted = false

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(TeardownOnFailureFixture::class.java))
            .execute()

        assertTrue(TeardownOnFailureFixture.teardownExecuted,
            "Teardown should have executed even though the step failed")
    }

    @Test
    fun `teardown executes on success`() {
        TeardownOnSuccessFixture.teardownExecuted = false

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(TeardownOnSuccessFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.succeeded(2)
                stats.failed(0)
            }

        assertTrue(TeardownOnSuccessFixture.teardownExecuted,
            "Teardown should have executed after successful step")
    }

    @Test
    fun `parameterized scenarios run all examples`() {
        ParameterizedFixture.executedValues.clear()

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(ParameterizedFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                // 2 examples x 3 steps = 6 step executions
                stats.started(6)
                stats.succeeded(6)
                stats.failed(0)
            }

        assertEquals(listOf(3, 9), ParameterizedFixture.executedValues,
            "Both examples should have been executed")
    }

    @Test
    fun `empty scenario has no test events`() {
        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(EmptyScenarioFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.started(0)
                stats.succeeded(0)
            }
    }

    @Test
    fun `single step scenario works`() {
        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(SingleStepFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.started(1)
                stats.succeeded(1)
            }
    }

    @Test
    fun `onFailure RUN continues execution after failure`() {
        OnFailureRunFixture.thirdStepRan = false

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(OnFailureRunFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.started(3)
                stats.failed(1) // the middle step
                stats.succeeded(2) // first and third
            }

        assertTrue(OnFailureRunFixture.thirdStepRan,
            "Third step should have run despite second step failing")
    }

    @Test
    fun `engine is discovered via SPI`() {
        // This test verifies that EngineTestKit can find our engine by ID,
        // which means the SPI descriptor file is working
        val events = EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(BasicScenarioFixture::class.java))
            .execute()
            .allEvents()

        // Should have engine started/finished events
        assertTrue(events.list().isNotEmpty(), "Engine should produce events")
    }

    @Test
    fun `steps execute in declaration order`() {
        StepOrderingFixture.executionOrder.clear()

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(StepOrderingFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.started(3)
                stats.succeeded(3)
            }

        assertEquals(listOf("first", "second", "third"), StepOrderingFixture.executionOrder,
            "Steps should execute in declaration order")
    }

    @Test
    fun `multiple teardowns on a single step all execute`() {
        MultipleTeardownsFixture.teardownOrder.clear()

        EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(MultipleTeardownsFixture::class.java))
            .execute()
            .testEvents()
            .assertStatistics { stats ->
                stats.succeeded(2)
                stats.failed(0)
            }

        assertEquals(listOf("teardown-1", "teardown-2"), MultipleTeardownsFixture.teardownOrder,
            "All teardowns should have executed in order")
    }

    @Test
    fun `teardown failure is suppressed when step also fails`() {
        val events = EngineTestKit
            .engine("kbehave")
            .selectors(selectClass(TeardownFailureSuppressedFixture::class.java))
            .execute()
            .testEvents()

        // The step fails (primary exception is from the step, teardown exception is suppressed)
        events.assertStatistics { stats ->
            stats.started(2)
            stats.failed(1)
            stats.aborted(1) // second step is skipped
        }
    }
}
