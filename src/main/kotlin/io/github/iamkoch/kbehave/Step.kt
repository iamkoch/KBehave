package io.github.iamkoch.kbehave

/**
 * Represents a single step in a scenario.
 */
data class Step(
    val description: String,
    val action: suspend () -> Unit,
    val skipReason: String? = null,
    val teardownActions: MutableList<suspend () -> Unit> = mutableListOf()
) {
    val isSkipped: Boolean get() = skipReason != null
}
