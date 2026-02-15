package io.github.iamkoch.kbehave

/**
 * Controls the behavior of subsequent steps when a step fails.
 *
 * Used with [StepDefinition.onFailure] or [StepBuilder.onFailure]:
 * ```kotlin
 * "When this might fail" x { riskyOp() } onFailure RemainingSteps.RUN
 * ```
 *
 * @see StepDefinition.onFailure
 * @see StepBuilder.onFailure
 */
enum class RemainingSteps {
    /**
     * Skip all remaining steps in the scenario (default).
     *
     * This is the standard BDD behavior: later steps typically depend on
     * earlier ones, so running them after a failure would produce misleading results.
     */
    SKIP,

    /**
     * Continue running remaining steps even after this step fails.
     *
     * Useful for cleanup or verification steps that should execute regardless
     * of earlier failures.
     */
    RUN
}
