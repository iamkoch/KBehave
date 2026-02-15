package io.github.iamkoch.kbehave

/**
 * Builder for creating scenario steps with pre-configured skip, teardown, and failure behavior.
 *
 * Obtain an instance via [String.step]:
 * ```kotlin
 * "Optional step".step().skip("Not ready yet") x { /* won't execute */ }
 * "Database step".step().teardown { db.close() } x { db.query("...") }
 * ```
 *
 * Unlike the direct [String.x] syntax, the builder lets you configure skip reason
 * and teardown actions *before* defining the step body.
 *
 * @param description The natural language description shown in the IDE test tree.
 */
class StepBuilder(private val description: String) {
    private var skipReason: String? = null
    private val teardownActions = mutableListOf<suspend () -> Unit>()
    private var failureBehavior: RemainingSteps = RemainingSteps.SKIP

    /**
     * Marks this step as skipped. The step body will not execute, and the
     * IDE test tree will show the step as skipped with the given [reason].
     *
     * @param reason Human-readable explanation shown in the test report.
     */
    fun skip(reason: String = "Step skipped"): StepBuilder {
        skipReason = reason
        return this
    }

    /**
     * Adds a teardown action that runs after this step completes, even if the step fails.
     * Multiple teardown actions can be added and will execute in registration order.
     *
     * @param action The suspend cleanup function.
     */
    fun teardown(action: suspend () -> Unit): StepBuilder {
        teardownActions.add(action)
        return this
    }

    /**
     * Controls what happens to subsequent steps when this step fails.
     *
     * By default steps use [RemainingSteps.SKIP], meaning a failure causes all
     * later steps to be skipped. Use [RemainingSteps.RUN] for cleanup steps
     * that must execute regardless.
     *
     * @param behavior [RemainingSteps.SKIP] or [RemainingSteps.RUN].
     */
    fun onFailure(behavior: RemainingSteps): StepBuilder {
        failureBehavior = behavior
        return this
    }

    /**
     * Defines the step action and registers the fully-configured step with the
     * current scenario. This is the terminal operation of the builder chain.
     *
     * @param action The suspend function containing the step logic.
     */
    infix fun x(action: suspend () -> Unit) {
        val step = Step(description, action, skipReason, teardownActions, failureBehavior)
        ScenarioContext.addStep(step)
    }
}
