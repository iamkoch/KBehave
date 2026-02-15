package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.RemainingSteps
import io.github.iamkoch.kbehave.Step
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor

/**
 * TestDescriptor for an individual step within a scenario.
 *
 * Each step appears as a separate test node in the test runner,
 * providing precise visibility into which steps pass or fail.
 */
internal class KBehaveStepDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    private val step: Step
) : AbstractTestDescriptor(
    uniqueId,
    displayName
) {

    var shouldSkip: Boolean = false
    var skipReason: String? = null

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    /**
     * Executes the step.
     *
     * @return true if step completed successfully, false if it failed
     */
    fun execute(): StepExecutionResult {
        // Check if step should be skipped
        if (shouldSkip || step.isSkipped) {
            val reason = skipReason ?: step.skipReason ?: "Previous step failed"
            println("  ↷ ${step.description} (SKIPPED: $reason)")
            return StepExecutionResult.Skipped(reason)
        }

        var stepException: Throwable? = null
        try {
            runBlocking {
                step.action()
            }
        } catch (e: Throwable) {
            stepException = e
        } finally {
            // Teardowns always run, even if the step failed
            for (teardown in step.teardownActions) {
                try {
                    runBlocking { teardown() }
                } catch (e: Throwable) {
                    if (stepException == null) {
                        stepException = e
                    } else {
                        stepException.addSuppressed(e)
                    }
                }
            }
        }

        return if (stepException != null) {
            println("  ✗ ${step.description}")
            val shouldSkipRemainingSteps = step.failureBehavior == RemainingSteps.SKIP
            StepExecutionResult.Failed(stepException, shouldSkipRemainingSteps)
        } else {
            println("  ✓ ${step.description}")
            StepExecutionResult.Success
        }
    }
}

/**
 * Result of executing a step.
 */
internal sealed class StepExecutionResult {
    object Success : StepExecutionResult()
    data class Skipped(val reason: String) : StepExecutionResult()
    data class Failed(val throwable: Throwable, val shouldSkipRemaining: Boolean) : StepExecutionResult()
}
