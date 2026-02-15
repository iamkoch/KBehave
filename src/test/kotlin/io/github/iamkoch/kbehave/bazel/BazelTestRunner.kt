package io.github.iamkoch.kbehave.bazel

import org.junit.platform.console.ConsoleLauncher
import kotlin.system.exitProcess

/**
 * Bazel wrapper for JUnit Platform Console Launcher.
 *
 * Reads TESTBRIDGE_TEST_ONLY environment variable (set by Bazel's --test_filter)
 * and converts Bazel test filters to JUnit Platform selector arguments before
 * delegating to ConsoleLauncher.
 *
 * This enables IntelliJ's Bazel plugin to run individual tests correctly.
 *
 * Supported filter formats:
 * - "ClassName" -> --select-class=package.ClassName
 * - "ClassName#methodName" -> --select-method=package.ClassName#methodName
 * - "package.ClassName" -> --select-class=package.ClassName
 * - "package.ClassName#methodName" -> --select-method=package.ClassName#methodName
 */
object BazelTestRunner {

    private const val DEFAULT_PACKAGE = "io.github.iamkoch.kbehave.examples"
    private const val ENV_TEST_FILTER = "TESTBRIDGE_TEST_ONLY"

    @JvmStatic
    fun main(args: Array<String>) {
        val testFilter = System.getenv(ENV_TEST_FILTER)

        val launcherArgs = if (testFilter.isNullOrBlank()) {
            // No filter - run all tests in package (default behavior)
            buildDefaultArgs(args)
        } else {
            // Convert Bazel filter to JUnit Platform selectors
            println("Bazel test filter: $testFilter")
            buildFilteredArgs(testFilter, args)
        }

        println("JUnit Platform args: ${launcherArgs.joinToString(" ")}")

        // Delegate to ConsoleLauncher's main method
        ConsoleLauncher.main(*launcherArgs)
    }

    private fun buildDefaultArgs(additionalArgs: Array<String>): Array<String> {
        return arrayOf(
            "--select-package=$DEFAULT_PACKAGE",
            "--details=tree",
            "--fail-if-no-tests",
            "--exclude-engine=junit-jupiter",
            *additionalArgs
        )
    }

    private fun buildFilteredArgs(testFilter: String, additionalArgs: Array<String>): Array<String> {
        val selectors = parseTestFilter(testFilter)

        return buildList {
            addAll(selectors)
            add("--details=tree")
            add("--fail-if-no-tests")
            add("--exclude-engine=junit-jupiter")
            // Override default classname filter when selecting specific methods
            // This is needed because JUnit Platform's default filter only matches classes
            // ending with "Test" or "Tests", and we need to match all classes when filtering by method
            add("--include-classname=.*")
            addAll(additionalArgs)
        }.toTypedArray()
    }

    /**
     * Parses Bazel test filter into JUnit Platform selector arguments.
     *
     * Handles multiple filters separated by colons (Bazel format).
     * Adds default package to simple class names.
     *
     * Supported formats from IntelliJ/Bazel:
     * - "ClassName" -> --select-class
     * - "ClassName#methodName" -> --select-method
     * - "ClassName.methodName$" -> --select-method (IntelliJ format with regex anchor)
     */
    private fun parseTestFilter(filter: String): List<String> {
        // Support colon-separated multiple filters (Bazel allows this)
        val filters = filter.split(":")

        return filters.flatMap { singleFilter ->
            var trimmed = singleFilter.trim()
            if (trimmed.isEmpty()) return@flatMap emptyList()

            // Remove regex anchor from end (IntelliJ adds $ for exact match)
            if (trimmed.endsWith("$")) {
                trimmed = trimmed.dropLast(1)
            }

            when {
                // Method selector with # separator: Class#method
                trimmed.contains("#") -> {
                    val (classPath, methodName) = trimmed.split("#", limit = 2)
                    val fullyQualifiedClass = if (classPath.startsWith(DEFAULT_PACKAGE)) {
                        classPath
                    } else {
                        "$DEFAULT_PACKAGE.$classPath"
                    }
                    listOf("--select-method=$fullyQualifiedClass#$methodName")
                }
                // Method selector with dot separator: Class.method (IntelliJ format)
                // Distinguish from package.Class by checking if it starts with uppercase after last dot
                trimmed.contains(".") && !trimmed.startsWith(DEFAULT_PACKAGE) -> {
                    val lastDotIndex = trimmed.indexOf(".")
                    val beforeDot = trimmed.substring(0, lastDotIndex)
                    val afterDot = trimmed.substring(lastDotIndex + 1)

                    // If the part after the first dot starts with lowercase, it's a method name
                    // Class names start with uppercase, method names with lowercase or backtick
                    if (afterDot.isNotEmpty() && (afterDot[0].isLowerCase() || afterDot[0] == '`')) {
                        // This is Class.method format
                        val fullyQualifiedClass = "$DEFAULT_PACKAGE.$beforeDot"
                        listOf("--select-method=$fullyQualifiedClass#$afterDot")
                    } else {
                        // This is a fully qualified class name
                        listOf("--select-class=$trimmed")
                    }
                }
                // Fully qualified class (contains DEFAULT_PACKAGE)
                trimmed.startsWith(DEFAULT_PACKAGE) -> {
                    listOf("--select-class=$trimmed")
                }
                // Simple class name
                else -> {
                    listOf("--select-class=$DEFAULT_PACKAGE.$trimmed")
                }
            }
        }
    }
}
