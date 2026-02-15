package io.github.iamkoch.kbehave

/**
 * Handle returned by [String.x] that allows chaining [teardown] and [onFailure]
 * modifiers onto a step.
 *
 * ```kotlin
 * "Given a resource" x {
 *     resource = openResource()
 * } teardown {
 *     resource.close()
 * } onFailure RemainingSteps.RUN
 * ```
 *
 * @see x
 * @see StepBuilder
 */
class StepDefinition internal constructor(private val step: Step) {
    /**
     * Adds a teardown action that runs after this step completes, even if the step fails.
     * Multiple teardowns can be chained and will execute in registration order.
     *
     * ```kotlin
     * "Given resources" x {
     *     db = connectToDb()
     *     file = openFile()
     * } teardown {
     *     db.close()
     * } teardown {
     *     file.close()
     * }
     * ```
     *
     * @param action The suspend cleanup function.
     * @return This [StepDefinition] for further chaining.
     */
    infix fun teardown(action: suspend () -> Unit): StepDefinition {
        step.teardownActions.add(action)
        return this
    }

    /**
     * Controls what happens to subsequent steps when this step fails.
     *
     * By default all steps use [RemainingSteps.SKIP]. Use [RemainingSteps.RUN]
     * for cleanup or verification steps that must always execute:
     * ```kotlin
     * "When this might fail" x { riskyOp() } onFailure RemainingSteps.RUN
     * ```
     *
     * @param behavior [RemainingSteps.SKIP] or [RemainingSteps.RUN].
     * @return A new [StepDefinition] with the updated failure behavior.
     */
    infix fun onFailure(behavior: RemainingSteps): StepDefinition {
        // Need to update the step's failureBehavior
        // Since Step is a data class, we need to replace it in the context
        val updatedStep = step.copy(failureBehavior = behavior)
        ScenarioContext.replaceLastStep(updatedStep)
        return StepDefinition(updatedStep)
    }
}

/**
 * Defines a test step with a natural language description.
 *
 * This is the core DSL of KBehave. The receiver `String` becomes the step's
 * description shown in the IDE test tree. The [action] lambda contains the
 * test logic for this step. Returns a [StepDefinition] for chaining
 * [StepDefinition.teardown] and [StepDefinition.onFailure].
 *
 * ```kotlin
 * "Given a calculator" x { calculator = Calculator() }
 * "When I add 2 and 3" x { result = calculator.add(2, 3) }
 * "Then the result should be 5" x { assertEquals(5, result) }
 * ```
 *
 * With teardown and failure control:
 * ```kotlin
 * "Given a resource" x { resource = open() } teardown { resource.close() }
 * "When it may fail" x { riskyOp() } onFailure RemainingSteps.RUN
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.x(action: suspend () -> Unit): StepDefinition {
    val step = Step(this, action)
    ScenarioContext.addStep(step)
    return StepDefinition(step)
}

/**
 * Creates a [StepBuilder] for this description, allowing skip, teardown, and
 * failure behavior to be configured before the step body.
 *
 * Use this when you need to skip a step or attach teardown actions before
 * defining the step body:
 * ```kotlin
 * "Optional step".step().skip("Not ready") x { /* won't execute */ }
 * "Database step".step().teardown { db.close() } x { db.query("...") }
 * ```
 *
 * For simple steps without pre-configuration, use [String.x] directly.
 *
 * @return A [StepBuilder] for configuring and defining this step.
 * @see StepBuilder
 * @see x
 */
fun String.step(): StepBuilder = StepBuilder(this)

/**
 * BDD alias for [x] — used for "Given" steps that set up preconditions.
 *
 * ```kotlin
 * "Given a calculator" given { calculator = Calculator() }
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.given(action: suspend () -> Unit) = this x action

/**
 * BDD alias for [x] — used for "When" steps that perform the action under test.
 *
 * The trailing underscore avoids a conflict with Kotlin's `when` keyword.
 *
 * ```kotlin
 * "When I add 2 and 3" when_ { result = calculator.add(2, 3) }
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.when_(action: suspend () -> Unit) = this x action

/**
 * BDD alias for [x] — used for "Then" steps that assert expected outcomes.
 *
 * ```kotlin
 * "Then the result should be 5" then { assertEquals(5, result) }
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.then(action: suspend () -> Unit) = this x action

/**
 * BDD alias for [x] — used for additional "And" continuation steps.
 *
 * ```kotlin
 * "And the calculator is reset" and { calculator.reset() }
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.and(action: suspend () -> Unit) = this x action

/**
 * BDD alias for [x] — used for "But" exception/negation steps.
 *
 * ```kotlin
 * "But the history is empty" but { assertTrue(calculator.history.isEmpty()) }
 * ```
 *
 * @param action The suspend function to execute for this step.
 * @return A [StepDefinition] that supports [StepDefinition.teardown] and [StepDefinition.onFailure] chaining.
 */
infix fun String.but(action: suspend () -> Unit) = this x action
