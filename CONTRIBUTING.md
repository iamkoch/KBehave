# Contributing to KBehave

Thank you for your interest in contributing to KBehave! We welcome contributions from the community.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/KBehave.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run the tests: `./gradlew test`
6. Commit your changes: `git commit -am 'Add some feature'`
7. Push to the branch: `git push origin feature/your-feature-name`
8. Create a Pull Request

## Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions focused and concise
- Add KDoc comments for public APIs

### Testing

- Write tests for all new features
- Ensure all tests pass before submitting a PR
- Add example tests to demonstrate new functionality
- Follow the existing test structure in `src/test/kotlin/io/github/iamkoch/kbehave/examples`

### DSL Changes

**IMPORTANT**: When making changes to the DSL:
- Always update the README.md with any DSL changes
- Include examples of the new syntax
- Update existing examples if the syntax changes

### Commit Messages

- Use clear, descriptive commit messages
- Start with a verb (Add, Fix, Update, Remove, etc.)
- Keep the first line under 50 characters
- Add details in the body if needed

Example:
```
Add support for custom step formatters

- Implement StepFormatter interface
- Add default formatter implementation
- Update documentation
```

## Pull Request Process

1. Ensure your code follows the project's coding style
2. Update the README.md with details of changes if applicable
3. Add or update tests as needed
4. Ensure all tests pass
5. Update documentation if you're changing functionality
6. Request review from maintainers

## Reporting Bugs

When reporting bugs, please include:

- A clear, descriptive title
- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Your environment (Kotlin version, JVM version, OS)
- Code samples if applicable

## Suggesting Features

We love new ideas! When suggesting features:

- Check if the feature has already been suggested
- Provide a clear use case
- Explain how it aligns with KBehave's philosophy
- Consider providing a proof-of-concept or example

## Questions?

Feel free to open an issue for any questions about contributing.

## License

By contributing to KBehave, you agree that your contributions will be licensed under the MIT License.
