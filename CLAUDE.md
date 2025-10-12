- always update the README.md with any DSL changes
- this is a faithful reproduction of https://github.com/adamralph/xbehave.net
- when unsure how to proceed, or how to complete incomplete features, first check the above repository to try to faithfully recreate the original behaviour
- whenever you finish a task or feature, use gradle to run the tests from the root of the repo

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "*.OnFailureScenarioTest"

# Build the library
./gradlew build
```
