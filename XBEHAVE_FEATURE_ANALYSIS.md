# xBehave Usage Analysis Report

## Executive Summary

This report analyzes xBehave.net usage patterns across the rev2 C# codebase and compares them against KBehave's current implementation. The analysis reviewed 15+ test files across multiple projects (gold, blue, orange, teal, red, yellow, ketchup) to identify commonly used features and patterns.

**Key Finding**: KBehave already implements all the core xBehave features found in the rev2 codebase. No critical features are missing.

---

## xBehave Features Found in rev2 Codebase

### 1. ✅ Basic Scenario Annotation - `[Scenario]`

**Status**: ✅ Fully Supported in KBehave

**Usage in rev2**: Standard annotation for marking test methods as scenarios.

**Examples from rev2**:
```csharp
// From: Rae.Gold.Lambdas.ApiRequestHandler.ClearBank.Tests/AccountCreatedTests.cs
[Scenario]
[DomainAutoData]
public void ClearBankWebhookTests(...)
{
    "".x(async () => { /* ... */ });
    "".x(() => { /* ... */ });
}

// From: Rae.Blue.Lambdas.CommandHandler.Tests/AddMobileNumberHandlerTests.cs
[Scenario]
[AutoData]
public void HandleCommandSuccessfullyAddsAMobileNumber(...)
{
    "Given a command envelope is received".x(() => { /* ... */ });
    "When a command has been received".x(async () => { /* ... */ });
    "Then domain events should be published".x(() => { /* ... */ });
}
```

**KBehave Equivalent**:
```kotlin
@Scenario
fun handleCommandSuccessfullyAddsAMobileNumber() {
    "Given a command envelope is received" x { /* ... */ }
    "When a command has been received" x { /* ... */ }
    "Then domain events should be published" x { /* ... */ }
}
```

---

### 2. ✅ Step Definition with `.x()` Extension

**Status**: ✅ Fully Supported in KBehave

**Usage in rev2**: Core DSL for defining test steps with natural language descriptions.

**Examples from rev2**:
```csharp
// From: Rae.Orange.Lambdas.DomainEventHandler.Tests/OptInAndOutDomainEventHandlerTests.cs
"Given a domain event has been published"
    .x(() => {
        _testOutputHelper.WriteLine(domainEvent.ToString());
        eventDispatcher.Register((database) => new CustomerOptedOutOfChannelEventHandler(...));
    });

"When it is received"
    .x(async () => {
        await new Function().HandleRecord(envelopeFixture.AsSQSMessage(), eventDispatcher);
    });

"Then an integration event should be raised"
    .x(() => {
        var events = fixture.PublicEvents;
        events.Count.ShouldBe(1);
    });
```

**KBehave Equivalent**:
```kotlin
"Given a domain event has been published" x {
    eventDispatcher.register { database -> CustomerOptedOutOfChannelEventHandler(...) }
}

"When it is received" x {
    function.handleRecord(envelopeFixture.asSQSMessage(), eventDispatcher)
}

"Then an integration event should be raised" x {
    val events = fixture.publicEvents
    events.count shouldBe 1
}
```

---

### 3. ✅ Async/Await Support

**Status**: ✅ Fully Supported in KBehave (via suspend functions)

**Usage in rev2**: Extensive use of async operations within steps.

**Examples from rev2**:
```csharp
// From: Rae.Gold.Lambdas.DomainEventHandler.Projections.Tests/ProjectionTests.cs
"Given a projection was saved".x(async () => {
    await saveProjection(projection);
});

"When I look it up by account number and sort code".x(async () => {
    retrieved = (await findProjection(proj =>
        proj.AccountNumber.Equals(projection.AccountNumber) &&
        proj.SortCode.Equals(projection.SortCode))).ValueOr(() => null);
});

// From: Rae.Teal.Lambdas.DomainEventHandler.ClearBank.Tests/ClearBankOutboundPaymentTests.cs
"When it is received"
    .x(async () => {
        await Function.HandleRecord(
            envelopeFixture.AsSQSMessage(),
            eventDispatcher,
            logger);
    });
```

**KBehave Equivalent**:
```kotlin
"Given a projection was saved" x {
    saveProjection(projection)
}

"When I look it up by account number and sort code" x {
    retrieved = findProjection { proj ->
        proj.accountNumber == projection.accountNumber &&
        proj.sortCode == projection.sortCode
    }.getOrNull()
}
```

---

### 4. ✅ Scenario Skip with Reason - `[Scenario(Skip = "reason")]`

**Status**: ✅ Fully Supported in KBehave

**Usage in rev2**: Used to temporarily disable tests, particularly for local-only or integration tests.

**Examples from rev2**:
```csharp
// From: Rae.Gold.Lambdas.DomainEventHandler.Projections.Tests/ProjectionTests.cs
[Scenario(Skip = "Run locally only")]
[DomainAutoData]
public void FindProjectionSucceeds(AccountProjection projection) { /* ... */ }

// From: Rae.Orange.Lambdas.IntegrationEventHandler.Tests/UnitOfWorkInboxTests.cs
[Scenario(Skip = "Run locally, can't be frigged trying to bring up mongo within the test framework")]
[DomainAutoData]
public void SuccessfullyProcessesEvents() { /* ... */ }

// From: Rae.Yellow.Lambdas.IntegrationEventHandler.Tests/DummyTest.cs
[Scenario(Skip = "Dummy test to check crm mappings")]
```

**KBehave Equivalent**:
```kotlin
// Note: KBehave uses @Disabled from JUnit 5 for scenario-level skipping
@Disabled("Run locally only")
@Scenario
fun findProjectionSucceeds(projection: AccountProjection) { /* ... */ }

// Or use step-level skipping:
@Scenario
fun findProjectionSucceeds() {
    "Given a projection".step().skip("Not ready") x { /* ... */ }
}
```

---

### 5. ✅ Display Name - `[Scenario(DisplayName = "...")]`

**Status**: ✅ Fully Supported in KBehave

**Usage in rev2**: Custom display names for test scenarios.

**Examples from rev2**:
```csharp
// From: Rae.Flexi.Bff.Web.Tests/Features/MarketingPreferencesFeature.cs
[Scenario(DisplayName = nameof(SavesSuccessfully))]
[WebhostAutoData]
public void SavesSuccessfully(...) { /* ... */ }
```

**KBehave Equivalent**:
```kotlin
@Scenario(displayName = "Saves Successfully")
fun savesSuccessfully() { /* ... */ }
```

---

### 6. ✅ Constructor Injection with ITestOutputHelper

**Status**: ✅ Supported (JUnit 5 TestInfo parameter)

**Usage in rev2**: xUnit's ITestOutputHelper for logging during tests.

**Examples from rev2**:
```csharp
// From: Rae.Orange.Lambdas.DomainEventHandler.Tests/OptInAndOutDomainEventHandlerTests.cs
public class OptInAndOutDomainEventHandlerTests
{
    private readonly ITestOutputHelper _testOutputHelper;

    public OptInAndOutDomainEventHandlerTests(ITestOutputHelper testOutputHelper)
    {
        _testOutputHelper = testOutputHelper;
    }

    [Scenario]
    [AutoData]
    public void SuccessfullyRaisesOptedOutIntegrationEvent(...)
    {
        "Given a domain event has been published"
            .x(() => {
                _testOutputHelper.WriteLine(domainEvent.ToString());
                // ...
            });
    }
}
```

**KBehave Equivalent**:
```kotlin
class OptInAndOutDomainEventHandlerTests {
    @Scenario
    fun successfullyRaisesOptedOutIntegrationEvent(testInfo: TestInfo) {
        "Given a domain event has been published" x {
            println(domainEvent.toString()) // or use a proper logger
            // ...
        }
    }
}
```

---

### 7. ⚠️ Integration with AutoFixture - `[AutoData]`, `[DomainAutoData]`

**Status**: ⚠️ Not Applicable (Framework-specific)

**Usage in rev2**: Heavy use of AutoFixture for test data generation.

**Examples from rev2**:
```csharp
// From: Rae.Blue.Lambdas.CommandHandler.Tests/AddMobileNumberHandlerTests.cs
[Scenario]
[AutoData]
public void HandleCommandSuccessfullyAddsAMobileNumber(
    AddProspectMobileNumberCommand command,
    DomainFixture<Prospect> domain,
    Prospect existingProspect)
{
    // AutoFixture auto-generates these parameters
}

// From: Rae.Gold.Lambdas.ApiRequestHandler.ClearBank.Tests/AccountCreatedTests.cs
[Scenario]
[DomainAutoData]
public void ClearBankWebhookTests(
    DomainFixture<Account> domainFixture,
    AccountNumberAddedEvent accountNumberAddedEvent,
    // ... many more auto-generated parameters
)
```

**Notes**:
- This is specific to the .NET ecosystem and AutoFixture library
- KBehave can use similar libraries like Instancio, jqwik, or custom fixture builders
- Not a KBehave feature gap, but an ecosystem difference

**KBehave Pattern** (using manual fixtures or libraries):
```kotlin
@Scenario
fun handleCommandSuccessfullyAddsAMobileNumber() {
    val command = AddProspectMobileNumberCommand.random()
    val domain = DomainFixture<Prospect>()
    val existingProspect = Prospect.random()

    // ... test steps
}
```

---

### 8. 🆕 Custom Extension: `.xOnEvent<T>()`

**Status**: ❌ Not Supported (Custom Extension for Boundary Tests)

**Usage in rev2**: Custom extension method for waiting/asserting on events in boundary tests.

**Examples from rev2**:
```csharp
// From: Rae.Gold.BoundaryTests/HappyPathFlowTests.cs
"Then I should receive an AccountCreatedForApplicationEvent"
    .xOnEvent<AccountCreatedForApplicationIntegrationEvent>(
        e => e.ApplicationId == applicationAcceptedEvent.ApplicationId,
        evt => resultingEvent = evt);

// From: Rae.Red.BoundaryTests/FlowTests.cs
"Then I should receive a VerificationStartedIntegrationEvent"
    .xOnEvent<IdentityDocumentProcessedIntegrationEvent>(
        correlationId,
        (evt) => resultingEvent = evt);
```

**Notes**:
- This is a **custom extension method** specific to the rev2 codebase
- It's not part of xBehave core functionality
- It appears to be used for integration/boundary testing with event streams
- Provides event polling/waiting functionality

**Recommendation**:
- This is not a KBehave feature gap
- Users can create similar custom extensions in Kotlin as needed
- Example implementation could be added to KBehave examples/documentation

**Possible KBehave Pattern**:
```kotlin
// Custom extension
infix fun <T> String.xOnEvent(eventHandler: suspend (T) -> Unit): StepDefinition {
    return this x {
        // Poll/wait for event logic
        val event = eventPoller.waitFor<T>()
        eventHandler(event)
    }
}

// Usage
"Then I should receive an account created event" xOnEvent<AccountCreatedEvent> { event ->
    resultingEvent = event
}
```

---

### 9. ✅ Empty String Steps (Anonymous Steps)

**Status**: ✅ Supported (though discouraged)

**Usage in rev2**: Using empty strings for steps when description is not important.

**Examples from rev2**:
```csharp
// From: Rae.Gold.Lambdas.ApiRequestHandler.ClearBank.Tests/AccountCreatedTests.cs
"".x(async () => {
    response = await new RequestHandler(...).Handle(request);
});

"".x(() => {
    var payload = JsonConvert.DeserializeObject<Response>(response.Body);
    payload.Nonce.ShouldBe(request.Nonce);
});
```

**KBehave Equivalent**:
```kotlin
"" x {
    response = requestHandler.handle(request)
}

"" x {
    val payload = Json.decodeFromString<Response>(response.body)
    payload.nonce shouldBe request.nonce
}
```

**Notes**: While supported, it's better practice to use descriptive strings for test readability.

---

### 10. ✅ Multiple Steps in Single Scenario

**Status**: ✅ Fully Supported

**Usage in rev2**: Standard pattern across all test files.

**Examples from rev2**:
```csharp
// From: Rae.Ketchup.Lambdas.SecondLevelRetry.Tests/RetryTests.cs
"Given a message is in retry state".x(async () => { /* ... */ });
"When the retry mechanism runs".x(async () => { /* ... */ });
"Then it should send it to the queue".x(() => { /* ... */ });
"And update the retry state to completed".x(() => { /* ... */ });
```

**KBehave Equivalent**:
```kotlin
"Given a message is in retry state" x { /* ... */ }
"When the retry mechanism runs" x { /* ... */ }
"Then it should send it to the queue" x { /* ... */ }
"And update the retry state to completed" x { /* ... */ }
```

---

## Features NOT Found in rev2 Codebase

These are xBehave features that were **not observed** in the rev2 codebase:

### 1. ❓ Step-Level Skip - `.Skip()`

**Status**: Not found in rev2, but **supported in KBehave**

xBehave supports skipping individual steps:
```csharp
"Optional step".x(() => { /* ... */ }).Skip("Not ready");
```

KBehave equivalent:
```kotlin
"Optional step".step().skip("Not ready") x { /* ... */ }
```

### 2. ❓ Step-Level Teardown - `.Teardown()`

**Status**: Not found in rev2, but **supported in KBehave**

xBehave supports teardown per step:
```csharp
"Given a resource".x(() => {
    resource = new Resource();
}).Teardown(() => {
    resource.Dispose();
});
```

KBehave equivalent:
```kotlin
"Given a resource" x {
    resource = Resource()
} teardown {
    resource.close()
}
```

### 3. ❓ Background Steps - `[Background]`

**Status**: Not found in rev2

xBehave supports `[Background]` attribute for common setup. This was not observed in the rev2 codebase.

### 4. ❓ Example/Theory Pattern - `[Example]`

**Status**: Not found in rev2

xBehave supports parameterized scenarios via `[Example]` attribute. Not observed in rev2, though KBehave supports this via `@Example` annotation.

---

## KBehave Feature Coverage

### ✅ Fully Covered Features

| xBehave Feature | KBehave Support | Notes |
|----------------|----------------|-------|
| `[Scenario]` | ✅ `@Scenario` | Same concept, Kotlin annotation syntax |
| `.x()` steps | ✅ `x` infix | Kotlin infix syntax, cleaner than extension method |
| Async/await | ✅ Suspend functions | Kotlin coroutines are more idiomatic |
| Skip scenario | ✅ `@Disabled` (JUnit 5) | JUnit 5 standard approach |
| Display name | ✅ `displayName` parameter | Supported on annotation |
| Multiple steps | ✅ Full support | Same behavior |
| Step skipping | ✅ `.step().skip()` | Same functionality |
| Teardown | ✅ `teardown` | Inline syntax, very similar |
| String interpolation | ✅ Native Kotlin | Better than C# string interpolation |

### 🆕 KBehave-Specific Features

These features exist in KBehave but weren't heavily used (or applicable) in rev2:

| Feature | Description |
|---------|-------------|
| `@Example` | Parameterized scenarios (not observed in rev2) |
| BDD aliases | `given`, `when_`, `then`, `and`, `but` (optional) |
| Suspend support | Full coroutine support throughout |

### ⚠️ Ecosystem Differences

| Aspect | xBehave (C#) | KBehave (Kotlin) |
|--------|--------------|------------------|
| Test framework | xUnit.net | JUnit 5 |
| Test data generation | AutoFixture | Manual or libraries (Instancio, jqwik) |
| Assertion library | Shouldly (in rev2) | Kotest, AssertJ, JUnit assertions |
| Async model | async/await | suspend functions |

---

## Recommendations

### 1. ✅ No Critical Features Missing

KBehave already implements all core xBehave features found in the rev2 codebase. Users migrating from xBehave will find equivalent functionality for all their test patterns.

### 2. 📚 Documentation Improvements

Consider adding these examples to KBehave documentation:

1. **Async/Suspend Examples**: Show more complex async patterns
2. **Test Data Generation**: Document patterns for fixture/factory usage
3. **Custom Extensions**: Show how to create custom step extensions like `.xOnEvent()`
4. **Migration Guide**: Create an xBehave → KBehave migration guide

### 3. 🔧 Optional Enhancements

Consider these optional additions:

**a) Custom Step Extensions Support**
```kotlin
// Make it easy for users to create custom step extensions
interface StepExtension {
    fun String.customStep(): StepBuilder
}
```

**b) Fixture/Factory Helpers**
```kotlin
// Optional helper for test data generation
@Fixture
annotation class Fixture

fun <T> fixture(): T = /* generate random instance */
```

**c) Example Documentation**
Add examples showing:
- Integration with test data libraries (Instancio, etc.)
- Custom assertion DSLs (Kotest, AssertJ)
- Event-driven testing patterns
- Boundary/integration test patterns

### 4. ✅ Feature Parity Achieved

KBehave has achieved feature parity with xBehave for all patterns observed in the rev2 production codebase. The remaining differences are:

1. **Ecosystem differences** (test frameworks, libraries) - by design
2. **Custom extensions** (like `.xOnEvent()`) - user-defined, not part of core xBehave
3. **Unused features** (like `@Example`) - already supported in KBehave

---

## Migration Examples

### Example 1: Basic Scenario with Async

**xBehave (C#)**:
```csharp
[Scenario]
[AutoData]
public void HandleCommandSuccessfullyAddsAMobileNumber(
    AddProspectMobileNumberCommand command,
    DomainFixture<Prospect> domain,
    Prospect existingProspect)
{
    Repository.SaveAggregate saveProspect = default;
    Envelope envelope = default;

    "Given a command envelope is received"
        .x(() => {
            saveProspect = domain.SaveAggregate;
            envelope = new Envelope {
                Type = typeof(AddProspectMobileNumberCommand).Name,
                Body = JsonConvert.SerializeObject(command)
            };
        });

    "When a command has been received"
        .x(async () => {
            await new Function().HandleRecord(
                envelope,
                new TestLambdaContext(),
                AddProspectMobileNumberHandlerFactory.Compose(
                    TestLogger.CreateLogger("test"),
                    loadProspect,
                    saveProspect));
        });

    "Then domain events should be published"
        .x(() => {
            var matchedEvent = domain.CapturedEvents
                .SingleOrDefault(e => e.GetType() == typeof(MobileNumberAddedDomainEvent));
            matchedEvent.ShouldNotBeNull();
        });
}
```

**KBehave (Kotlin)**:
```kotlin
@Scenario
fun handleCommandSuccessfullyAddsAMobileNumber() {
    val command = AddProspectMobileNumberCommand.random()
    val domain = DomainFixture<Prospect>()
    val existingProspect = Prospect.random()

    lateinit var saveProspect: Repository.SaveAggregate
    lateinit var envelope: Envelope

    "Given a command envelope is received" x {
        saveProspect = domain.saveAggregate
        envelope = Envelope(
            type = AddProspectMobileNumberCommand::class.simpleName!!,
            body = Json.encodeToString(command)
        )
    }

    "When a command has been received" x {
        Function().handleRecord(
            envelope,
            TestLambdaContext(),
            AddProspectMobileNumberHandlerFactory.compose(
                TestLogger.createLogger("test"),
                loadProspect,
                saveProspect
            )
        )
    }

    "Then domain events should be published" x {
        val matchedEvent = domain.capturedEvents
            .singleOrNull { it is MobileNumberAddedDomainEvent }
        matchedEvent.shouldNotBeNull()
    }
}
```

### Example 2: Skip and DisplayName

**xBehave (C#)**:
```csharp
[Scenario(Skip = "Run locally only", DisplayName = "Find Projection Test")]
[DomainAutoData]
public void FindProjectionSucceeds(AccountProjection projection)
{
    "Given a projection was saved".x(async () => {
        await saveProjection(projection);
    });

    "When I look it up".x(async () => {
        retrieved = await findProjection(projection.Id);
    });
}
```

**KBehave (Kotlin)**:
```kotlin
@Disabled("Run locally only")
@Scenario(displayName = "Find Projection Test")
fun findProjectionSucceeds() {
    val projection = AccountProjection.random()
    lateinit var retrieved: AccountProjection

    "Given a projection was saved" x {
        saveProjection(projection)
    }

    "When I look it up" x {
        retrieved = findProjection(projection.id)
    }
}
```

---

## Conclusion

**KBehave successfully provides feature parity with xBehave.net** for all patterns observed in the rev2 production codebase. The library is production-ready for teams migrating from xBehave or starting new Kotlin projects.

### Key Strengths
- ✅ All core xBehave features supported
- ✅ Idiomatic Kotlin syntax (infix, suspend, etc.)
- ✅ Excellent JUnit 5 integration
- ✅ Clean, readable test syntax

### Areas for Enhancement (Optional)
- 📚 More documentation/examples
- 📚 Migration guide from xBehave
- 🔧 Helper utilities for common patterns

The analysis of 15+ production test files across multiple domains confirms that KBehave is feature-complete for real-world BDD testing scenarios.
