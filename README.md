# KBehave

[![CI](https://github.com/iamkoch/KBehave/actions/workflows/ci.yml/badge.svg)](https://github.com/iamkoch/KBehave/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.iamkoch/kbehave.svg)](https://search.maven.org/artifact/io.github.iamkoch/kbehave)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)

A Kotlin version of [xBehave.net](https://github.com/adamralph/xbehave.net) - a JUnit 5 extension for describing each step in a test with natural language.

## Overview

KBehave allows you to write BDD-style tests in Kotlin with a clean, expressive syntax. Each test scenario consists of multiple steps described in natural language, making your tests self-documenting and easy to understand.

## Features

- ✅ **Natural Language Steps**: Describe test steps using plain English (or any language)
- ✅ **Parameterized Scenarios**: Run the same scenario with different inputs using `@Example`
- ✅ **Step Execution Control**: Skip steps and automatic skipping of remaining steps on failure
- ✅ **Teardown Support**: Automatic cleanup after step execution
- ✅ **Suspend Function Support**: Full support for Kotlin coroutines
- ✅ **JUnit 5 Integration**: Works seamlessly with existing JUnit 5 test infrastructure

## Getting Started

### Installation

Add KBehave to your `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("io.github.iamkoch:kbehave:1.0.0")
}
```

### Basic Usage

```kotlin
import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertEquals

class CalculatorTest {

    @Scenario
    fun `simple addition`() {
        var calculator: Calculator? = null
        var result = 0

        "Given a calculator" x {
            calculator = Calculator()
        }

        "When I add 2 and 3" x {
            result = calculator!!.add(2, 3)
        }

        "Then the result should be 5" x {
            assertEquals(5, result)
        }
    }
}
```

## Syntax

### The `x` DSL

The core of KBehave is the `x` infix function on `String`:

```kotlin
"Step description" x {
    // Step implementation
}
```

The `x` function returns a `StepDefinition` that supports chaining with `teardown`:

```kotlin
"Step with cleanup" x {
    // Step implementation
} teardown {
    // Cleanup code
}
```

### String Interpolation

Use Kotlin's string interpolation for dynamic step descriptions:

```kotlin
val x = 10
val y = 5

"Given the numbers $x and $y" x { }
"When I multiply them together" x { result = x * y }
"Then the result should be ${x * y}" x { assertEquals(50, result) }
```

## Parameterized Scenarios

Use `@Example` annotations to run the same scenario with different parameters:

```kotlin
@Scenario
@Example(intValues = [1, 2, 3])
@Example(intValues = [2, 3, 5])
@Example(intValues = [5, 7, 12])
fun `addition with examples`(x: Int, y: Int, expected: Int) {
    var result = 0

    "Given the numbers $x and $y" x { }

    "When I add them together" x {
        result = Calculator().add(x, y)
    }

    "Then the result should be $expected" x {
        assertEquals(expected, result)
    }
}
```

The `@Example` annotation supports different parameter types:
- `intValues` for `Int` parameters
- `longValues` for `Long` parameters
- `stringValues` for `String` parameters
- `booleanValues` for `Boolean` parameters
- `doubleValues` for `Double` parameters

You can mix different types in the same example:
```kotlin
@Example(stringValues = ["Alice"], intValues = [25])
@Example(stringValues = ["Bob"], intValues = [30])
fun `user scenario`(name: String, age: Int) {
    // ...
}
```

This will generate three separate test scenarios, one for each `@Example`.

## Teardown

Add teardown actions that execute after a step completes using the inline `teardown` syntax:

```kotlin
@Scenario
fun `scenario with cleanup`() {
    var resource: Resource? = null

    "Given a resource" x {
        resource = Resource()
        resource!!.open()
    } teardown {
        resource?.close()
    }

    "When I use the resource" x {
        resource!!.doSomething()
    }

    "Then it will be cleaned up automatically" x {
        // Teardown already executed after the first step
    }
}
```

You can also chain multiple teardown actions and combine with `onFailure`:

```kotlin
"Given a database connection" x {
    connection = connectToDb()
} teardown {
    connection?.close()
} teardown {
    cleanupTempFiles()
} onFailure RemainingSteps.RUN
```

## Skipping Steps

Skip individual steps with an optional reason:

```kotlin
@Scenario
fun `scenario with skipped step`() {
    "Given this step executes" x {
        println("This executes")
    }

    "When this step is skipped".step()
        .skip("Feature not yet implemented") x {
        println("This won't execute")
    }

    "Then this step also executes" x {
        println("This executes too")
    }
}
```

## Controlling Step Execution on Failure

By default, when a step fails, all remaining steps are skipped. You can control this behavior using the `onFailure` syntax:

```kotlin
@Scenario
fun `teardown runs even on failure`() {
    var cleanupExecuted = false

    "Given a setup step" x {
        setupResource()
    }

    "When this step fails" x {
        throw Exception("Something went wrong")
    } onFailure RemainingSteps.RUN  // Continue to next steps

    "Then cleanup still happens" x {
        cleanupExecuted = true
        cleanup()
    } onFailure RemainingSteps.RUN
}
```

The `onFailure` infix function accepts a `RemainingSteps` enum:
- `RemainingSteps.SKIP` (default) - Skip remaining steps if this step fails
- `RemainingSteps.RUN` - Continue executing remaining steps even if this step fails

This is particularly useful for ensuring teardown steps always run, or for scenarios where you want to collect multiple failure points.

## Step Execution Model

KBehave follows these execution rules:

1. **Sequential Execution**: Steps execute in the order they're defined
2. **Fail Fast (Default)**: If a step fails (throws an exception), remaining steps are skipped by default
3. **Configurable Failure Behavior**: Use `onFailure RemainingSteps.RUN` to continue execution after failures
4. **Teardown Guaranteed**: Teardown actions always execute, even if the step fails
5. **Visual Feedback**: Step execution is logged to the console:
   - `✓` - Step passed
   - `✗` - Step failed
   - `↷` - Step skipped

### Test Reporting

Currently, KBehave reports test results at the **scenario level** (not individual steps) in most IDEs and test runners. This means:

- ✅ Each `@Scenario` appears as a runnable test in your IDE
- ✅ Step execution details are printed to the console output
- ✅ You can see which steps passed/failed in the console
- ⚠️  Steps do NOT appear as individual test nodes in the IDE test tree (unlike xBehave.net)

To see detailed step-by-step execution:
1. Run the tests
2. Check the console output for the `✓`, `✗`, and `↷` symbols showing each step's status

**Note**: Full IDE integration with individual step reporting (like xBehave.net's test tree) requires a custom JUnit Platform TestEngine, which is currently in development.

## Advanced Features

### Suspend Functions

KBehave fully supports Kotlin coroutines:

```kotlin
@Scenario
suspend fun `async scenario`() {
    "Given an async operation" x {
        delay(100)
    }

    "When I await the result" x {
        val result = async { fetchData() }.await()
    }
}
```

### Alternative Step Syntax

While the default `x` DSL is recommended, you can use alternative patterns:

```kotlin
// Using the step builder for skip functionality
"Optional feature".step().skip("Not ready yet") x {
    println("Won't execute")
}

// Using BDD aliases (if preferred)
"Given a calculator" given { calculator = Calculator() }
"When I add numbers" when_ { result = calculator.add(1, 2) }
"Then the result is correct" then { assertEquals(3, result) }
```

### Combining Features

You can combine teardown with other features:

```kotlin
@Scenario
@Example(intValues = [100, 200])
@Example(intValues = [300, 400])
fun `parameterized with teardown`(amount: Int, expected: Int) {
    var transaction: Transaction? = null

    "Given a transaction of $amount" x {
        transaction = startTransaction(amount)
    } teardown {
        transaction?.rollback()
    }

    "Then the transaction is processed" x {
        assertEquals(expected, transaction!!.total)
    }
}
```

## Comparison with xBehave.net

| Feature | xBehave.net | KBehave |
|---------|-------------|---------|
| Basic scenarios | `[Scenario]` + `.x()` | `@Scenario` + `x` |
| Parameterized tests | `[Example(...)]` | `@Example(...)` |
| Skip steps | `.Skip()` | `.skip()` |
| Teardown | `.Teardown()` | `teardown` |
| Failure behavior | `.OnFailure()` | `onFailure` |
| Test framework | xUnit.net | JUnit 5 |
| Language | C# | Kotlin |
| Async support | async/await | suspend functions |

## Examples

Check out the [example tests](src/test/kotlin/io/github/iamkoch/kbehave/examples) for more usage patterns:

- [BasicScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/BasicScenarioTest.kt) - Basic scenario syntax
- [ParameterizedScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/ParameterizedScenarioTest.kt) - Using `@Example`
- [TeardownScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/TeardownScenarioTest.kt) - Resource cleanup
- [SkipScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/SkipScenarioTest.kt) - Skipping steps
- [OnFailureScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/OnFailureScenarioTest.kt) - Controlling execution on failure

## Building

```bash
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Philosophy

Like xBehave.net, KBehave follows these principles:

1. **Tests as Documentation**: Each step should read like a specification
2. **Fail Fast**: Stop at the first failure to make debugging easier
3. **Minimal Ceremony**: Clean, expressive syntax without boilerplate
4. **Framework Integration**: Works with standard testing tools and runners

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [xBehave.net](https://github.com/adamralph/xbehave.net) by Adam Ralph for the original concept and design
- The JUnit 5 team for the extensible testing platform

---

Made with ❤️ for the Kotlin community
