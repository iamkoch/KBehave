package io.github.iamkoch.kbehave

/**
 * Thread-local context for tracking steps during scenario execution.
 */
object ScenarioContext {
    private val steps = ThreadLocal<MutableList<Step>>()

    fun addStep(step: Step) {
        getSteps().add(step)
    }

    fun getSteps(): MutableList<Step> {
        return steps.get() ?: mutableListOf<Step>().also { steps.set(it) }
    }

    fun clear() {
        steps.remove()
    }
}
