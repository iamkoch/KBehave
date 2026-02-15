# KBehave

[![CI](https://github.com/iamkoch/KBehave/actions/workflows/ci.yml/badge.svg)](https://github.com/iamkoch/KBehave/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.iamkoch/kbehave.svg)](https://search.maven.org/artifact/io.github.iamkoch/kbehave)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg)](https://kotlinlang.org)

A Kotlin port of [xBehave.net](https://github.com/adamralph/xbehave.net) - describe each step of a test in natural language, with every step visible as a separate node in your IDE's test tree.

## Why KBehave?

Most test frameworks show a single pass/fail per test method. When a test with 10 lines fails, you read console output to find which assertion broke.

KBehave gives you **step-level visibility**. Each step appears as a child node in IntelliJ's test tree with its own green/red/amber status. You see exactly which step failed at a glance, without reading logs.

```
CalculatorTest
  └─ simple addition
      ├─ Given a calculator     ✓
      ├─ When I add 2 and 3     ✓
      └─ Then the result is 5   ✗
```

This is achieved via a custom JUnit Platform TestEngine - no Jupiter extension hacks. It works with standard Gradle and IntelliJ test runners out of the box.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("io.github.iamkoch:kbehave:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    testImplementation 'io.github.iamkoch:kbehave:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.iamkoch</groupId>
    <artifactId>kbehave</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## Getting Started

```kotlin
import io.github.iamkoch.kbehave.Scenario
import io.github.iamkoch.kbehave.x
import org.junit.jupiter.api.Assertions.assertEquals

class CalculatorTest {

    @Scenario
    fun `simple addition`() {
        lateinit var calculator: Calculator
        var result = 0

        "Given a calculator" x {
            calculator = Calculator()
        }

        "When I add 2 and 3" x {
            result = calculator.add(2, 3)
        }

        "Then the result should be 5" x {
            assertEquals(5, result)
        }
    }
}
```

That's it. No test runner configuration, no extension annotations. The KBehave TestEngine is discovered automatically via JUnit Platform's SPI.

## The `x` DSL

The core of KBehave is the `x` infix function on `String`:

```kotlin
"Step description" x {
    // step implementation
}
```

### Teardown

Add cleanup actions that always execute after the step, even if it fails:

```kotlin
"Given a resource" x {
    resource = Resource()
    resource.open()
} teardown {
    resource.close()
}
```

Multiple teardowns can be chained:

```kotlin
"Given resources" x {
    db = connectToDb()
    file = openFile()
} teardown {
    db.close()
} teardown {
    file.close()
}
```

### Failure Behavior

By default, if a step fails, remaining steps are skipped. Override this per step:

```kotlin
"When this might fail" x {
    riskyOperation()
} onFailure RemainingSteps.RUN  // continue to next steps
```

### Skipping Steps

```kotlin
"When this feature is ready".step()
    .skip("Not implemented yet") x {
    // won't execute
}
```

## Parameterized Scenarios

Run the same scenario with different inputs using `@Example`:

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

Each `@Example` creates a separate sub-container in the test tree with its own steps. Supported parameter types: `intValues`, `longValues`, `stringValues`, `booleanValues`, `doubleValues`.

> **Note:** Each `@Example` should use a single value type. When mixing types (e.g. `intValues` and `stringValues` in the same annotation), parameters are extracted in a fixed order (int, long, string, boolean, double) which may not match your method signature.

## Suspend Function Support

Steps fully support Kotlin coroutines:

```kotlin
@Scenario
suspend fun `async scenario`() {
    "Given an async operation" x {
        delay(100)
    }
}
```

## Configuration

KBehave works out of the box with standard JUnit Platform setup. If your project also runs JUnit Jupiter tests, both engines coexist - Jupiter handles `@Test` methods, KBehave handles `@Scenario` methods.

### Gradle

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

No `excludeEngines` or `includeEngines` configuration is needed.

## BDD Aliases

For teams that prefer explicit BDD keywords:

```kotlin
"Given a calculator" given { calculator = Calculator() }
"When I add numbers" when_ { result = calculator.add(1, 2) }
"Then the result is correct" then { assertEquals(3, result) }
```

## Coming from xBehave.net?

| xBehave.net | KBehave |
|-------------|---------|
| `[Scenario]` + `.x()` | `@Scenario` + `x` |
| `[Example(...)]` | `@Example(...)` |
| `.Skip("reason")` | `.step().skip("reason")` |
| `.Teardown(() => ...)` | `teardown { ... }` |
| `.OnFailure(RemainingSteps.Run)` | `onFailure RemainingSteps.RUN` |
| async/await | suspend functions |
| xUnit.net | JUnit 5 |

## Troubleshooting

**Steps don't appear as child nodes in IntelliJ:**
Ensure `@Scenario` does NOT have `@Test` or `@TestTemplate` as meta-annotations. KBehave uses its own TestEngine.

**TestEngine not discovered:**
Verify `META-INF/services/org.junit.platform.engine.TestEngine` exists in the JAR and contains `io.github.iamkoch.kbehave.engine.KBehaveTestEngine`.

**NoSuchMethodException during test instance creation:**
KBehave requires a no-arg constructor. For Spring Boot tests with DI, use JUnit Jupiter instead.

**Side effects running twice:**
KBehave invokes scenario methods during both the discovery and execution phases. Code outside `x { }` lambdas (e.g. variable assignments, database connections) will execute twice. Keep side effects inside the `x { }` block:

```kotlin
// Bad - connectToDb() runs twice:
@Scenario
fun test() {
    val db = connectToDb()
    "query" x { db.query() }
}

// Good - connectToDb() runs only during execution:
@Scenario
fun test() {
    lateinit var db: Database
    "Given a database connection" x { db = connectToDb() }
    "When I query" x { db.query() }
}
```

## Examples

See [src/test/kotlin/.../examples](src/test/kotlin/io/github/iamkoch/kbehave/examples) for runnable examples:

- [BasicScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/BasicScenarioTest.kt) - Core syntax
- [ParameterizedScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/ParameterizedScenarioTest.kt) - `@Example` usage
- [TeardownScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/TeardownScenarioTest.kt) - Resource cleanup
- [SkipScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/SkipScenarioTest.kt) - Skipping steps
- [OnFailureScenarioTest.kt](src/test/kotlin/io/github/iamkoch/kbehave/examples/OnFailureScenarioTest.kt) - Failure behavior control

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

- [xBehave.net](https://github.com/adamralph/xbehave.net) by Adam Ralph for the original concept
- The JUnit 5 team for the extensible testing platform
