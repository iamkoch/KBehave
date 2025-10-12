package io.github.iamkoch.kbehave

/**
 * Builder for creating scenario steps with support for skip and teardown.
 */
class StepBuilder(private val description: String) {
    private var skipReason: String? = null
    private val teardownActions = mutableListOf<suspend () -> Unit>()
    private var failureBehavior: RemainingSteps = RemainingSteps.SKIP

    /**
     * Marks this step as skipped with an optional reason.
     */
    fun skip(reason: String = "Step skipped"): StepBuilder {
        skipReason = reason
        return this
    }

    /**
     * Adds a teardown action to be executed after the step.
     */
    fun teardown(action: suspend () -> Unit): StepBuilder {
        teardownActions.add(action)
        return this
    }

    /**
     * Defines the behavior of remaining steps if this step fails.
     */
    fun onFailure(behavior: RemainingSteps): StepBuilder {
        failureBehavior = behavior
        return this
    }

    /**
     * Defines the step action and registers it with the current scenario context.
     */
    infix fun x(action: suspend () -> Unit) {
        val step = Step(description, action, skipReason, teardownActions, failureBehavior)
        ScenarioContext.addStep(step)
    }
}
