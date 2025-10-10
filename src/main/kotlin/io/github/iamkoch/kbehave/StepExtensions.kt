package io.github.iamkoch.kbehave

/**
 * Wrapper class to enable teardown syntax after step definition.
 */
class StepDefinition(private val step: Step) {
    /**
     * Adds a teardown action to be executed after the step completes.
     *
     * Usage:
     * ```kotlin
     * "Given a resource" x { openResource() } teardown { closeResource() }
     * ```
     */
    infix fun teardown(action: suspend () -> Unit): StepDefinition {
        step.teardownActions.add(action)
        return this
    }
}

/**
 * Extension function that allows natural language step definitions.
 *
 * Usage:
 * ```kotlin
 * "Given a calculator" x { calculator = Calculator() }
 * "Given a resource" x { openResource() } teardown { closeResource() }
 * ```
 */
infix fun String.x(action: suspend () -> Unit): StepDefinition {
    val step = Step(this, action)
    ScenarioContext.addStep(step)
    return StepDefinition(step)
}

/**
 * Creates a step builder that supports skip and teardown.
 *
 * Usage:
 * ```kotlin
 * "Optional step".step().skip("Not ready") x { /* won't execute */ }
 * "Database step".step().teardown { cleanup() } x { doWork() }
 * ```
 */
fun String.step(): StepBuilder = StepBuilder(this)

/**
 * Alias for common BDD step patterns
 */
infix fun String.given(action: suspend () -> Unit) = this x action
infix fun String.when_(action: suspend () -> Unit) = this x action
infix fun String.then(action: suspend () -> Unit) = this x action
infix fun String.and(action: suspend () -> Unit) = this x action
infix fun String.but(action: suspend () -> Unit) = this x action
