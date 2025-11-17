package io.github.iamkoch.kbehave.engine

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
 * Handles execution of the scenario by creating an instance of the test class
 * and invoking the scenario method, which will register steps via the 'x' infix function.
 */
class KBehaveScenarioDescriptor(
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

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    /**
     * Executes the scenario.
     *
     * This creates an instance of the test class (if needed), invokes the scenario method
     * to collect steps, then executes each step. The actual step execution logic is
     * delegated to the existing ScenarioExtension/ScenarioExecutionExtension code.
     */
    fun execute() {
        ScenarioContext.clear()

        try {
            // Create instance of test class (handles constructor injection for Spring/etc)
            val instance = createTestInstance()

            // Invoke the scenario method to collect steps
            val kotlinFunction = method.kotlinFunction

            if (kotlinFunction != null && kotlinFunction.isSuspend) {
                runBlocking {
                    kotlinFunction.callSuspend(instance)
                }
            } else {
                method.invoke(instance)
            }

            // Execute the collected steps
            val steps = ScenarioContext.getSteps()
            var skipRemaining = false

            for (step in steps) {
                if (skipRemaining || step.isSkipped) {
                    val skipReason = step.skipReason ?: "Previous step failed"
                    println("  ↷ ${step.description} (SKIPPED: $skipReason)")
                    continue
                }

                try {
                    println("  ✓ ${step.description}")
                    runBlocking {
                        step.action()
                        step.teardownActions.forEach { it() }
                    }
                } catch (e: Throwable) {
                    println("  ✗ ${step.description}")
                    if (step.failureBehavior == io.github.iamkoch.kbehave.RemainingSteps.SKIP) {
                        skipRemaining = true
                    }
                    throw e
                }
            }
        } finally {
            ScenarioContext.clear()
        }
    }

    /**
     * Creates an instance of the test class.
     *
     * For simple classes, uses the no-arg constructor.
     * For classes with constructor parameters (like Spring Boot tests with @Autowired),
     * this would need to be enhanced to work with dependency injection frameworks.
     *
     * Note: For Spring Boot tests, the existing JUnit Jupiter + ScenarioExtension
     * approach handles DI. This TestEngine is for non-Spring scenarios or when
     * we want full control over discovery.
     */
    private fun createTestInstance(): Any {
        // Try no-arg constructor first
        return try {
            testClass.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            // If no no-arg constructor, we need DI framework support
            // For now, throw a helpful error
            throw IllegalStateException(
                """
                KBehave TestEngine: ${testClass.simpleName} requires constructor parameters.

                For Spring Boot tests, use the JUnit Jupiter engine (which KBehave already supports).
                The TestEngine is primarily for plain Kotlin tests without DI requirements.

                To use with Spring Boot, ensure JUnit Jupiter is on the classpath and let it
                handle discovery via @TestTemplate (KBehave's @Scenario annotation uses this).
                """.trimIndent(),
                e
            )
        }
    }
}
