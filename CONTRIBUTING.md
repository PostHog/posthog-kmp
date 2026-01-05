# Contributing to PostHog KMP

Thank you for your interest in contributing to PostHog KMP! This document provides guidelines and information about contributing to this project.

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please be respectful and constructive in all interactions.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/nicepics/posthog-kmp/issues)
2. If not, create a new issue with:
   - A clear, descriptive title
   - Steps to reproduce the behavior
   - Expected vs actual behavior
   - Platform and version information
   - Code samples if applicable

### Suggesting Features

1. Check existing issues and discussions for similar suggestions
2. Create a new issue with the `enhancement` label
3. Describe the feature and its use case
4. Explain how it benefits the community

### Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `./gradlew allTests`
6. Commit with clear messages
7. Push and create a Pull Request

## Development Setup

### Prerequisites

- JDK 17+
- Android SDK (for Android development)
- Xcode (for iOS/macOS development on macOS)
- Node.js (for JS targets)

### Building

```bash
# Clone the repository
git clone https://github.com/nicepics/posthog-kmp.git
cd posthog-kmp

# Build all targets
./gradlew build

# Run tests
./gradlew allTests

# Build specific targets
./gradlew :posthog-kmp:compileKotlinAndroid
./gradlew :posthog-kmp:compileKotlinIosArm64
./gradlew :posthog-kmp:compileKotlinJs
```

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and small
- Use `explicit api` mode (compiler will enforce visibility modifiers)

### Testing

- Write unit tests for new functionality
- Test on multiple platforms when possible
- Include both positive and negative test cases

## Project Structure

```
posthog-kmp/
├── posthog-kmp/
│   └── src/
│       ├── commonMain/       # Shared API and models
│       ├── commonTest/       # Shared tests
│       ├── androidMain/      # Android implementation
│       ├── iosMain/          # iOS implementation
│       ├── macosMain/        # macOS implementation
│       ├── jvmMain/          # JVM implementation
│       ├── jsMain/           # JavaScript implementation
│       └── wasmJsMain/       # WebAssembly JS implementation
├── gradle/
│   └── libs.versions.toml    # Version catalog
├── build.gradle.kts          # Root build configuration
└── settings.gradle.kts       # Project settings
```

## Adding Platform Support

If you're adding support for a new platform:

1. Add the target in `build.gradle.kts`
2. Create the platform source set directory
3. Implement all `expect` functions from `PostHog.kt`
4. Add platform-specific tests
5. Update the README with platform information

## Release Process

Releases are managed by maintainers. To request a release:

1. Ensure all tests pass
2. Update the version in `gradle.properties`
3. Update CHANGELOG.md
4. Create a PR with the changes
5. Tag the release after merge

## Getting Help

- Check existing [Issues](https://github.com/nicepics/posthog-kmp/issues)
- Start a [Discussion](https://github.com/nicepics/posthog-kmp/discussions)
- Review [PostHog Documentation](https://posthog.com/docs)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
