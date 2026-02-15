package io.github.iamkoch.kbehave.engine

import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.ScenarioContext
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.PackageSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor

/**
 * JUnit Platform TestEngine for KBehave.
 *
 * This engine automatically discovers test classes containing @Scenario methods,
 * regardless of class naming conventions. Each step within a scenario is reported
 * as a separate test node, providing xBehave.net-style reporting where you can see
 * exactly which step failed.
 *
 * Discovery process:
 * 1. Scans classpath for classes with @Scenario methods
 * 2. Creates test descriptors for each class and scenario
 * 3. Discovers steps within each scenario
 * 4. Reports each step execution individually
 */
internal class KBehaveTestEngine : TestEngine {

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
            discoverClass(selector.getJavaClass(), engineDescriptor)
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
        } finally {
            ScenarioContext.clear()
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
                        val anyFailed = if (child.children.any { it is KBehaveExampleDescriptor }) {
                            executeChildren(child, listener)
                            false // container result is inferred from children by platform
                        } else {
                            executeScenarioSteps(child, listener)
                        }
                        if (anyFailed) {
                            listener.executionFinished(child, TestExecutionResult.failed(
                                AssertionError("Scenario failed: one or more steps failed")
                            ))
                        } else {
                            listener.executionFinished(child, TestExecutionResult.successful())
                        }
                    } catch (t: Throwable) {
                        listener.executionFinished(child, TestExecutionResult.failed(t))
                    } finally {
                        ScenarioContext.clear()
                    }
                }
                is KBehaveExampleDescriptor -> {
                    listener.executionStarted(child)
                    try {
                        val anyFailed = executeScenarioSteps(child, listener)
                        if (anyFailed) {
                            listener.executionFinished(child, TestExecutionResult.failed(
                                AssertionError("Example failed: one or more steps failed")
                            ))
                        } else {
                            listener.executionFinished(child, TestExecutionResult.successful())
                        }
                    } catch (t: Throwable) {
                        listener.executionFinished(child, TestExecutionResult.failed(t))
                    } finally {
                        ScenarioContext.clear()
                    }
                }
            }
        }
    }

    /**
     * @return true if any step failed
     */
    private fun executeScenarioSteps(scenarioDescriptor: TestDescriptor, listener: EngineExecutionListener): Boolean {
        var skipRemaining = false
        var anyFailed = false

        scenarioDescriptor.children.filterIsInstance<KBehaveStepDescriptor>().forEach { stepDescriptor ->
            if (skipRemaining) {
                stepDescriptor.shouldSkip = true
                stepDescriptor.skipReason = "Previous step failed"
            }

            listener.executionStarted(stepDescriptor)

            val result = stepDescriptor.execute()

            when (result) {
                is StepExecutionResult.Success -> {
                    listener.executionFinished(stepDescriptor, TestExecutionResult.successful())
                }
                is StepExecutionResult.Skipped -> {
                    listener.executionFinished(
                        stepDescriptor,
                        TestExecutionResult.aborted(null)
                    )
                }
                is StepExecutionResult.Failed -> {
                    anyFailed = true
                    listener.executionFinished(
                        stepDescriptor,
                        TestExecutionResult.failed(result.throwable)
                    )
                    if (result.shouldSkipRemaining) {
                        skipRemaining = true
                    }
                }
            }
        }
        return anyFailed
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

                // Discover steps immediately so the complete hierarchy exists for IntelliJ
                scenarioDescriptor.discoverSteps()
            }
        }
    }

    private fun discoverPackage(packageName: String, parent: TestDescriptor) {
        // Scan classpath for classes in the specified package
        val classLoader = Thread.currentThread().contextClassLoader
        val packagePath = packageName.replace('.', '/')

        try {
            val resources = classLoader.getResources(packagePath)
            resources.asIterator().forEach { url ->
                val classes = findClassesInPackage(packageName, url)
                classes.forEach { clazz ->
                    discoverClass(clazz, parent)
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail discovery
            System.err.println("Error discovering package $packageName: ${e.message}")
        }
    }

    private fun findClassesInPackage(packageName: String, packageUrl: java.net.URL): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        val packagePath = packageName.replace('.', '/')

        when (packageUrl.protocol) {
            "file" -> {
                val packageDir = java.io.File(packageUrl.toURI())
                if (packageDir.exists() && packageDir.isDirectory) {
                    packageDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.endsWith(".class")) {
                            val className = packageName + "." + file.name.substringBeforeLast(".class")
                            try {
                                val clazz = Class.forName(className)
                                if (clazz.declaredMethods.any { it.isAnnotationPresent(Scenario::class.java) }) {
                                    classes.add(clazz)
                                }
                            } catch (e: Exception) {
                                // Skip classes that can't be loaded
                            }
                        }
                    }
                }
            }
            "jar" -> {
                // Handle JAR files
                val jarPath = packageUrl.path.substringAfter("file:").substringBefore("!")
                try {
                    java.util.jar.JarFile(jarPath).use { jar ->
                        jar.entries().asIterator().forEach { entry ->
                            if (!entry.isDirectory && entry.name.endsWith(".class") && entry.name.startsWith(packagePath)) {
                                val className = entry.name.replace("/", ".").substringBeforeLast(".class")
                                try {
                                    val clazz = Class.forName(className)
                                    if (clazz.declaredMethods.any { it.isAnnotationPresent(Scenario::class.java) }) {
                                        classes.add(clazz)
                                    }
                                } catch (e: Exception) {
                                    // Skip classes that can't be loaded
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid JARs
                }
            }
        }
        return classes
    }

    private fun discoverClasspathRoot(root: java.net.URI, parent: TestDescriptor) {
        try {
            val rootDir = java.io.File(root)
            if (!rootDir.exists() || !rootDir.isDirectory) return

            rootDir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".class") }
                .filter { !it.name.contains('$') } // skip inner classes
                .forEach { classFile ->
                    val relativePath = classFile.relativeTo(rootDir).path
                    val className = relativePath
                        .removeSuffix(".class")
                        .replace(java.io.File.separatorChar, '.')

                    try {
                        val clazz = Class.forName(className)
                        discoverClass(clazz, parent)
                    } catch (_: Exception) {
                        // Skip classes that can't be loaded
                    }
                }
        } catch (_: Exception) {
            // Skip roots that can't be scanned
        }
    }
}
