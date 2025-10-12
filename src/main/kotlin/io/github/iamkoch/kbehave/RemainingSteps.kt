package io.github.iamkoch.kbehave

/**
 * Indicates the behavior of remaining steps when a step fails.
 */
enum class RemainingSteps {
    /**
     * Skip remaining steps.
     */
    SKIP,

    /**
     * Run remaining steps.
     */
    RUN
}
