package io.github.iamkoch.kbehave

/**
 * Represents a single step in a scenario.
 *
 * @property description The natural language description of the step.
 * @property action The suspend function to execute for this step.
 * @property skipReason If non-null, the step will be skipped with this reason.
 * @property teardownActions Actions to run after the step completes, even on failure.
 * @property failureBehavior Controls whether remaining steps are skipped or run on failure.
 */
internal data class Step(
    val description: String,
    val action: suspend () -> Unit,
    val skipReason: String? = null,
    val teardownActions: MutableList<suspend () -> Unit> = mutableListOf(),
    val failureBehavior: RemainingSteps = RemainingSteps.SKIP
) {
    val isSkipped: Boolean get() = skipReason != null
}
