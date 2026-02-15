package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.Example
import io.github.iamkoch.kbehave.ScenarioContext
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

/**
 * TestDescriptor for a single @Example within a parameterized @Scenario.
 *
 * Each example creates a separate container with its own set of steps,
 * allowing each parameterized run to be independently visible in the test tree.
 */
internal class KBehaveExampleDescriptor(
    uniqueId: UniqueId,
    displayName: String,
    private val testClass: Class<*>,
    private val method: Method,
    private val args: Array<Any>
) : AbstractTestDescriptor(
    uniqueId,
    displayName,
    MethodSource.from(testClass, method)
) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    override fun mayRegisterTests(): Boolean = true

    fun discoverSteps() {
        ScenarioContext.clear()

        try {
            val instance = testClass.getDeclaredConstructor().newInstance()
            val kotlinFunction = method.kotlinFunction

            if (kotlinFunction != null && kotlinFunction.isSuspend) {
                runBlocking {
                    if (args.isEmpty()) {
                        kotlinFunction.callSuspend(instance)
                    } else {
                        kotlinFunction.callSuspend(instance, *args)
                    }
                }
            } else {
                if (args.isEmpty()) {
                    method.invoke(instance)
                } else {
                    method.invoke(instance, *args)
                }
            }

            val steps = ScenarioContext.getSteps()
            steps.forEachIndexed { index, step ->
                val stepDescriptor = KBehaveStepDescriptor(
                    uniqueId.append("step", "${index + 1}: ${step.description}"),
                    step.description,
                    step
                )
                addChild(stepDescriptor)
            }
        } finally {
            ScenarioContext.clear()
        }
    }
}
