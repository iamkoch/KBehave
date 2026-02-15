# KBehave Specification (Internal Architecture Document)

> **Note:** This is an internal architecture document for contributors. For user-facing documentation, see [README.md](README.md).

## High-Level Objectives

KBehave is a Kotlin BDD testing framework that provides xBehave.net-style test reporting for the JVM. The framework has three primary objectives:

### 1. Individual Scenario Execution
- **Requirement**: Each scenario can be run individually via CLI or IDE (IntelliJ)
- **Implementation**: Custom JUnit Platform TestEngine with proper test selection support
- **Validation**: `./gradlew test --tests "*.specificScenario"` runs only that scenario

### 2. Hierarchical Test Display
- **Requirement**: Each scenario appears in the IDE as a test with the test function name, with child test nodes for each step
- **Implementation**: Scenarios are CONTAINER nodes, steps are TEST nodes in the JUnit Platform descriptor tree
- **Validation**: IntelliJ test runner shows: Class → Scenario → Step hierarchy

### 3. Step Status Visualization
- **Requirement**: Step execution status is clearly visible
  - Failed steps → red
  - Unreached/skipped steps → amber/no color/skipped status
- **Implementation**: Each step reports individual execution results (success/failure/skipped) to JUnit Platform
- **Validation**: IDE test tree shows proper colors for each step status

## Architecture

### JUnit Platform TestEngine

KBehave uses a **custom TestEngine** implementation (`KBehaveTestEngine`) rather than a JUnit Jupiter extension. This is the only approach that achieves hierarchical step display in IDEs.

#### Why TestEngine instead of Extension?

| Approach | Step Hierarchy | Individual Step Status | Complexity |
|----------|---------------|------------------------|------------|
| TestEngine | ✅ Yes | ✅ Yes | Medium |
| Jupiter Extension | ❌ No | ❌ No | Low |

Extensions execute all steps as a single test, making it impossible for IDEs to display individual step status.

### Test Descriptor Hierarchy

```
EngineDescriptor (type: ENGINE, id: [engine:kbehave])
  └─ KBehaveClassDescriptor (type: CONTAINER, id: [engine:kbehave]/[class:com.example.TestClass])
      └─ KBehaveScenarioDescriptor (type: CONTAINER, id: [...]/[method:testMethod])
          ├─ KBehaveStepDescriptor (type: TEST, id: [...]/[step:1: Given...])
          ├─ KBehaveStepDescriptor (type: TEST, id: [...]/[step:2: When...])
          └─ KBehaveStepDescriptor (type: TEST, id: [...]/[step:3: Then...])
```

#### Descriptor Types
- **Engine**: Entry point, registered via service provider
- **Class**: Container for scenarios, uses `ClassSource`
- **Scenario**: Container for steps, uses `MethodSource`
- **Step**: Individual test node, executes one step's action

#### UniqueId Format

The UniqueId format enables precise test selection:
- Engine: `[engine:kbehave]`
- Class: `[engine:kbehave]/[class:com.example.TestClass]`
- Scenario: `[engine:kbehave]/[class:com.example.TestClass]/[method:scenarioMethod]`
- Step: `[engine:kbehave]/[class:com.example.TestClass]/[method:scenarioMethod]/[step:1: Given...]`

This allows test runners to select:
- All tests: Run the engine
- All scenarios in a class: Select the class descriptor
- One scenario: Select the method descriptor
- One step: Select the step descriptor (for debugging)

## Discovery and Execution Flow

### Discovery Phase

```
1. JUnit Platform calls KBehaveTestEngine.discover()
   ↓
2. Engine scans for classes with @Scenario methods
   ↓
3. For each class, create KBehaveClassDescriptor
   ↓
4. For each @Scenario method, create KBehaveScenarioDescriptor
   ↓
5. For each scenario, invoke the method to collect steps
   ↓
6. For each step, create KBehaveStepDescriptor
   ↓
7. Return complete descriptor tree to JUnit Platform
```

**Critical Implementation Detail**: Steps must be discovered during the discovery phase (not execution phase). This is achieved by invoking the scenario method during discovery to capture the `"string" x { }` DSL calls.

### Execution Phase

```
1. JUnit Platform calls KBehaveTestEngine.execute()
   ↓
2. For each KBehaveScenarioDescriptor
   ↓
3. Report scenario started
   ↓
4. For each already-discovered KBehaveStepDescriptor
   ↓
5. Check if step should be skipped (due to previous failure)
   ↓
6. Report step started
   ↓
7. Execute step's action lambda
   ↓
8. Execute teardown actions
   ↓
9. Report step result (success/failure/skipped)
   ↓
10. If step fails and failureBehavior == SKIP, mark remaining steps to skip
   ↓
11. Report scenario finished
```

## Critical Invariants

These invariants MUST be maintained for the framework to work correctly:

### 1. NO @Test or @TestTemplate on @Scenario
**Why**: These annotations cause JUnit Jupiter to compete with KBehaveTestEngine, resulting in:
- Dual execution paths
- IDE confusion about which engine to use
- Loss of hierarchical step display

**Correct**:
```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Scenario(val displayName: String = "")
```

**Incorrect**:
```kotlin
@Test  // ❌ Creates engine conflict!
annotation class Scenario(val displayName: String = "")
```

### 2. Step Discovery During Discovery Phase
**Why**: JUnit Platform requires all tests to be registered before execution begins. Steps are discovered by invoking the scenario method during the discovery phase.

**Implementation**: `KBehaveScenarioDescriptor.discoverSteps()` is called from `KBehaveTestEngine.discover()`

**Consequence**: Scenario methods are invoked TWICE:
1. Once during discovery (to collect steps)
2. Once during execution (implicitly via step lambdas)

### 3. Scenarios are CONTAINER, Steps are TEST
**Why**: This is what creates the hierarchical display in IDEs.

**Implementation**:
```kotlin
class KBehaveScenarioDescriptor : AbstractTestDescriptor(...) {
    override fun getType() = TestDescriptor.Type.CONTAINER
    override fun mayRegisterTests() = true
}

class KBehaveStepDescriptor : AbstractTestDescriptor(...) {
    override fun getType() = TestDescriptor.Type.TEST
}
```

### 4. One Step Failure Can Skip Remaining Steps
**Why**: BDD scenarios often have dependencies between steps (e.g., "Given" steps set up state for "When" steps).

**Implementation**:
- Each step has a `failureBehavior` property (default: `RemainingSteps.SKIP`)
- When a step fails with `SKIP` behavior, subsequent steps are marked as skipped
- Use `onFailure RemainingSteps.RUN` for cleanup/teardown steps that must always run

**Example**:
```kotlin
"Given a resource is created" x {
    resource.create()
}

"When the resource is used" x {
    resource.use()  // Skipped if previous step failed
}

"Then cleanup the resource" x {
    resource.cleanup()
} onFailure RemainingSteps.RUN  // Always runs, even if previous steps failed
```

### 5. TestEngine Registration via Service Provider
**Why**: JUnit Platform discovers test engines via Java's ServiceLoader mechanism.

**Implementation**: `/src/main/resources/META-INF/services/org.junit.platform.engine.TestEngine`
```
io.github.iamkoch.kbehave.engine.KBehaveTestEngine
```

**Consequence**: If this file is missing or incorrect, the engine won't be discovered.

## IDE Integration Requirements

For proper IntelliJ IDEA integration:

1. **Engine Discovery**: IntelliJ must find the TestEngine via service provider file
2. **Descriptor Hierarchy**: The descriptor tree must be correctly typed (CONTAINER vs TEST)
3. **UniqueId Format**: Must use standard JUnit Platform format with proper segment types
4. **Test Selection**: Each descriptor must have a unique ID that can be used for filtering
5. **Result Reporting**: Each step must report its execution result individually

### Verifying IDE Integration

**Expected Behavior**:
- Green play button appears next to each @Scenario method
- Clicking play runs ONLY that scenario (not all tests)
- Test tree shows: Class → Scenario → Steps
- Green checkmark for passed steps
- Red X for failed steps
- Amber/gray for skipped steps

**If play button runs all tests**:
- Check for `@Test` or `@TestTemplate` on `@Scenario` (should not exist)
- Verify no other test engines are interfering
- Check IntelliJ's run configuration (should use JUnit Platform)

**If steps don't appear as child nodes**:
- Verify `KBehaveScenarioDescriptor.getType()` returns `CONTAINER`
- Verify `KBehaveScenarioDescriptor.mayRegisterTests()` returns `true`
- Verify `KBehaveStepDescriptor.getType()` returns `TEST`
- Ensure `discoverSteps()` is called during discovery phase

## CLI Testing

### Run All Tests
```bash
cd lib/kbehave
./gradlew test
```

### Run Specific Class
```bash
./gradlew test --tests "*OnFailureScenarioTest"
```

### Run Specific Scenario
```bash
./gradlew test --tests "*OnFailureScenarioTest.demonstration of onFailure RUN behavior"
```

### Verbose Output
```bash
./gradlew test --tests "*OnFailureScenarioTest" --info
```

## Common Issues and Solutions

### Issue: Clicking play runs all tests instead of one scenario
**Cause**: `@Test` or `@TestTemplate` annotation on `@Scenario` causes engine conflict

**Solution**: Remove the annotation from `@Scenario`. Only use the bare `@Scenario` annotation.

### Issue: Steps don't appear as child test nodes
**Cause**: Scenario descriptor is typed as TEST instead of CONTAINER

**Solution**: Ensure `KBehaveScenarioDescriptor.getType()` returns `TestDescriptor.Type.CONTAINER` and `mayRegisterTests()` returns `true`.

### Issue: Test engine not discovered
**Cause**: Missing or incorrect service provider file

**Solution**: Verify `/src/main/resources/META-INF/services/org.junit.platform.engine.TestEngine` exists and contains the correct fully-qualified class name.

### Issue: Steps not discovered
**Cause**: `discoverSteps()` not called during discovery phase

**Solution**: Ensure `KBehaveTestEngine.discover()` calls `scenarioDescriptor.discoverSteps()` before returning the descriptor tree.

### Issue: NoSuchMethodException during test instance creation
**Cause**: Test class requires constructor parameters (e.g., Spring Boot tests with DI)

**Solution**: KBehave's TestEngine currently only supports no-arg constructors. For Spring Boot tests with DI, consider using a test instance factory or restructuring tests to use companion objects for shared state.

## Future Enhancements

Potential improvements while maintaining the current architecture:

1. **Dependency Injection Support**: Integrate with Spring TestContext or create adapter for DI frameworks
2. **Parallel Step Execution**: Allow independent steps to run in parallel (requires explicit opt-in)
3. **Step Retry**: Automatic retry for flaky tests with configurable retry policy
4. **Custom Test Sources**: Richer source information for better IDE navigation
5. **Tags/Categories**: JUnit Platform tag support for filtering scenarios
6. **Dynamic Tests**: Support for generating scenarios at runtime (similar to `@TestFactory`)

## References

- **xBehave.net**: Original inspiration for KBehave's architecture
  - Repository: https://github.com/adamralph/xbehave.net
  - Key concepts: Scenarios as containers, steps as tests, xUnit integration
- **JUnit Platform**: Test engine implementation guide
  - Documentation: https://junit.org/junit5/docs/current/user-guide/#launcher-api-engines-custom
  - Key concepts: TestEngine, TestDescriptor, UniqueId
