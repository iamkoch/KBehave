# Contributing to KBehave

Thank you for your interest in contributing to KBehave!

## Building

```bash
./gradlew build
```

## Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "*OnFailureScenarioTest"

# Specific scenario
./gradlew test --tests "*OnFailureScenarioTest.demonstration of onFailure RUN behavior"
```

## Architecture

KBehave uses a custom JUnit Platform **TestEngine** (not a Jupiter extension) to achieve step-level IDE visibility. Key architecture docs:

- [SPEC.md](SPEC.md) - Internal architecture specification, test descriptor hierarchy, and critical invariants
- [XBEHAVE_FEATURE_ANALYSIS.md](XBEHAVE_FEATURE_ANALYSIS.md) - Feature parity analysis with xBehave.net

### Key Source Files

| File | Purpose |
|------|---------|
| `Scenario.kt` | `@Scenario` annotation |
| `StepExtensions.kt` | `x` infix function (public DSL) |
| `StepBuilder.kt` | Builder for skip/teardown before step definition |
| `engine/KBehaveTestEngine.kt` | TestEngine SPI implementation |
| `engine/KBehaveScenarioDescriptor.kt` | Scenario discovery and step collection |
| `engine/KBehaveStepDescriptor.kt` | Step execution with teardown/failure handling |

### Public API Surface

The following types are public API (stable after 1.0.0):

- `@Scenario`, `@Example`, `RemainingSteps` - Annotations and enums
- `String.x()`, `String.step()` - DSL entry points
- `StepDefinition` - Returned from `x`, supports `teardown` and `onFailure` chaining
- `StepBuilder` - Returned from `step()`, supports `skip`, `teardown`, `onFailure`
- BDD aliases: `given`, `when_`, `then`, `and`, `but`

Everything in the `engine` package and `Step`/`ScenarioContext` are `internal`.

## Development Guidelines

- Follow Kotlin coding conventions
- Write tests for all new features
- Add KDoc comments for public API additions
- Update README.md with any DSL changes
- When unsure about behavior, check [xBehave.net](https://github.com/adamralph/xbehave.net) for reference

## Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Run the tests: `./gradlew test`
5. Commit with a clear message
6. Push and create a Pull Request

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
