package io.github.iamkoch.kbehave

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method
import java.util.stream.Stream
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

/**
 * JUnit 5 extension that executes KBehave scenarios.
 *
 * This extension:
 * - Discovers scenarios annotated with @Scenario
 * - Handles @Example annotations for parameterized scenarios
 * - Executes each step as a separate test
 * - Skips remaining steps if a step fails
 * - Executes teardown actions
 */
class ScenarioExtension : TestTemplateInvocationContextProvider {

    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return context.testMethod
            .map { it.isAnnotationPresent(Scenario::class.java) }
            .orElse(false)
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val method = context.requiredTestMethod
        val examples = method.getAnnotationsByType(Example::class.java)

        return if (examples.isEmpty()) {
            // No examples, run once with no parameters
            Stream.of(createInvocationContext(method, emptyArray()))
        } else {
            // Run once for each example
            examples.map { example ->
                val values = extractValues(example)
                createInvocationContext(method, values)
            }.stream()
        }
    }

    private fun extractValues(example: Example): Array<Any> {
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

    private fun createInvocationContext(method: Method, args: Array<out Any>): TestTemplateInvocationContext {
        return object : TestTemplateInvocationContext {
            override fun getDisplayName(invocationIndex: Int): String {
                val scenario = method.getAnnotation(Scenario::class.java)
                val baseName = scenario.displayName.ifEmpty { method.name }
                return if (args.isNotEmpty()) {
                    "$baseName [${args.joinToString(", ")}]"
                } else {
                    baseName
                }
            }

            override fun getAdditionalExtensions(): List<Extension> {
                return listOf(
                    ScenarioExecutionExtension(args),
                    ParameterResolver(args)
                )
            }
        }
    }
}

/**
 * Resolves parameters for scenario methods (from @Example annotations).
 */
class ParameterResolver(private val args: Array<out Any>) : org.junit.jupiter.api.extension.ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.index < args.size
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return args[parameterContext.index]
    }
}

/**
 * Executes the scenario and its steps.
 */
class ScenarioExecutionExtension(private val args: Array<out Any>) :
    InvocationInterceptor, BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        ScenarioContext.clear()
    }

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        // Execute the scenario method to collect steps
        ScenarioContext.clear()

        try {
            val method = invocationContext.executable
            val kotlinFunction = method.kotlinFunction
            val target = invocationContext.target.orElse(null)

            if (kotlinFunction != null && kotlinFunction.isSuspend) {
                runBlocking {
                    if (args.isEmpty()) {
                        kotlinFunction.callSuspend(target)
                    } else {
                        kotlinFunction.callSuspend(target, *args)
                    }
                }
            } else {
                invocation.proceed()
            }

            // Now execute each step
            val steps = ScenarioContext.getSteps()
            executeSteps(steps, extensionContext)
        } finally {
            ScenarioContext.clear()
        }
    }

    private fun executeSteps(steps: List<Step>, extensionContext: ExtensionContext) {
        var skipRemaining = false

        for ((index, step) in steps.withIndex()) {
            if (skipRemaining || step.isSkipped) {
                val skipReason = step.skipReason ?: "Previous step failed"
                println("  ↷ ${step.description} (SKIPPED: $skipReason)")
                continue
            }

            try {
                println("  ✓ ${step.description}")
                runBlocking {
                    step.action()
                    // Execute teardown actions
                    step.teardownActions.forEach { it() }
                }
            } catch (e: Throwable) {
                println("  ✗ ${step.description}")
                skipRemaining = true
                throw e
            }
        }
    }

    override fun afterEach(context: ExtensionContext) {
        ScenarioContext.clear()
    }
}
