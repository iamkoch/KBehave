package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.Example
import io.github.iamkoch.kbehave.Scenario
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
 * TestDescriptor for a single @Scenario method.
 *
 * Acts as a container for individual step descriptors (non-parameterized)
 * or example descriptors (parameterized with @Example), creating a test tree
 * where each step is a separate test node for precise failure reporting.
 */
internal class KBehaveScenarioDescriptor(
    uniqueId: UniqueId,
    private val testClass: Class<*>,
    private val method: Method
) : AbstractTestDescriptor(
    uniqueId,
    getDisplayName(method),
    MethodSource.from(testClass, method)
) {
    companion object {
        private fun getDisplayName(method: Method): String {
            val scenario = method.getAnnotation(Scenario::class.java)
            return scenario?.displayName?.ifEmpty { method.name } ?: method.name
        }
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    override fun mayRegisterTests(): Boolean = true

    /**
     * Discovers steps for this scenario during the discovery phase.
     * Required by JUnit Platform - all tests must be discovered before execution.
     */
    fun discoverSteps() {
        val examples = method.getAnnotationsByType(Example::class.java)

        if (examples.isEmpty()) {
            // No examples - discover steps directly as children
            discoverStepsForParameters(emptyArray())
        } else {
            // Multiple examples - create a sub-container per example
            examples.forEachIndexed { index, example ->
                val values = extractValues(example)
                val exampleName = formatExampleName(values, index)
                val exampleDescriptor = KBehaveExampleDescriptor(
                    uniqueId.append("example", exampleName),
                    exampleName,
                    testClass,
                    method,
                    values
                )
                addChild(exampleDescriptor)
                exampleDescriptor.discoverSteps()
            }
        }
    }

    private fun formatExampleName(values: Array<Any>, index: Int): String {
        return "Example #${index + 1}: [${values.joinToString(", ")}]"
    }

    internal fun extractValues(example: Example): Array<Any> {
        val values = mutableListOf<Any>()

        if (example.intValues.isNotEmpty()) {
            values.addAll(example.intValues.toTypedArray())
        }
        if (example.longValues.isNotEmpty()) {
            values.addAll(example.longValues.toTypedArray())
        }
        if (example.stringValues.isNotEmpty()) {
            values.addAll(example.stringValues)
        }
        if (example.booleanValues.isNotEmpty()) {
            values.addAll(example.booleanValues.toTypedArray())
        }
        if (example.doubleValues.isNotEmpty()) {
            values.addAll(example.doubleValues.toTypedArray())
        }

        return values.toTypedArray()
    }

    private fun discoverStepsForParameters(args: Array<Any>) {
        ScenarioContext.clear()

        try {
            val instance = createTestInstance()
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

    private fun createTestInstance(): Any {
        return try {
            testClass.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(
                """
                KBehave TestEngine: ${testClass.simpleName} requires constructor parameters.

                For Spring Boot tests, disable the KBehave engine and use the JUnit Jupiter engine instead.
                Add this to your test class: @DisabledIfSystemProperty(named = "junit.jupiter.extensions.autodetection.enabled", matches = ".*")
                Or configure Gradle to exclude the KBehave engine for Spring tests.
                """.trimIndent(),
                e
            )
        }
    }
}
