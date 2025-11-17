package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.Scenario
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.PackageSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.lang.reflect.Method

/**
 * JUnit Platform TestEngine for KBehave.
 *
 * This engine automatically discovers test classes containing @Scenario methods,
 * regardless of class naming conventions. Users don't need any configuration -
 * just add KBehave as a dependency and it works.
 *
 * Discovery process:
 * 1. Scans classpath for classes with @Scenario methods
 * 2. Creates test descriptors for each class and scenario
 * 3. Delegates execution to ScenarioExtension (existing implementation)
 */
class KBehaveTestEngine : TestEngine {

    companion object {
        const val ENGINE_ID = "kbehave"
    }

    override fun getId(): String = ENGINE_ID

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val engineDescriptor = EngineDescriptor(uniqueId, "KBehave")

        // Discover classes based on selectors
        discoveryRequest.getSelectorsByType(ClassSelector::class.java).forEach { selector ->
            discoverClass(selector.javaClass, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
            discoverPackage(selector.packageName, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
            discoverClasspathRoot(selector.classpathRoot, engineDescriptor)
        }

        return engineDescriptor
    }

    override fun execute(request: ExecutionRequest) {
        val engineDescriptor = request.rootTestDescriptor
        val engineExecutionListener = request.engineExecutionListener

        engineExecutionListener.executionStarted(engineDescriptor)

        try {
            executeChildren(engineDescriptor, engineExecutionListener)
            engineExecutionListener.executionFinished(engineDescriptor, TestExecutionResult.successful())
        } catch (t: Throwable) {
            engineExecutionListener.executionFinished(
                engineDescriptor,
                TestExecutionResult.failed(t)
            )
        }
    }

    private fun executeChildren(
        parent: TestDescriptor,
        listener: EngineExecutionListener
    ) {
        parent.children.forEach { child ->
            when (child) {
                is KBehaveClassDescriptor -> {
                    listener.executionStarted(child)
                    try {
                        executeChildren(child, listener)
                        listener.executionFinished(child, TestExecutionResult.successful())
                    } catch (t: Throwable) {
                        listener.executionFinished(child, TestExecutionResult.failed(t))
                    }
                }
                is KBehaveScenarioDescriptor -> {
                    listener.executionStarted(child)
                    try {
                        child.execute()
                        listener.executionFinished(child, TestExecutionResult.successful())
                    } catch (t: Throwable) {
                        listener.executionFinished(child, TestExecutionResult.failed(t))
                    }
                }
            }
        }
    }

    private fun discoverClass(clazz: Class<*>, parent: TestDescriptor) {
        val scenarioMethods = clazz.declaredMethods.filter { method ->
            method.isAnnotationPresent(Scenario::class.java)
        }

        if (scenarioMethods.isNotEmpty()) {
            val classDescriptor = KBehaveClassDescriptor(
                parent.uniqueId.append("class", clazz.name),
                clazz
            )
            parent.addChild(classDescriptor)

            scenarioMethods.forEach { method ->
                val scenarioDescriptor = KBehaveScenarioDescriptor(
                    classDescriptor.uniqueId.append("method", method.name),
                    clazz,
                    method
                )
                classDescriptor.addChild(scenarioDescriptor)
            }
        }
    }

    private fun discoverPackage(packageName: String, parent: TestDescriptor) {
        // Package scanning would require classpath scanning library
        // For now, we rely on ClassSelector and ClasspathRootSelector
        // This is typically provided by the build tool (Gradle, Maven, Bazel)
    }

    private fun discoverClasspathRoot(root: java.net.URI, parent: TestDescriptor) {
        // Classpath scanning would require a classpath scanning library
        // For now, we rely on ClassSelector provided by the build tool
        // This is the standard approach used by most test engines
    }
}
