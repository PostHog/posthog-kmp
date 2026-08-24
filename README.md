# PostHog KMP

[![Maven Central](https://img.shields.io/maven-central/v/com.posthog/posthog-kmp)](https://central.sonatype.com/artifact/com.posthog/posthog-kmp)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The PostHog **Kotlin Multiplatform** SDK, supporting Android, iOS, Web (JS and Wasm), and JVM (desktop) from shared Kotlin code. PostHog is an open source platform for product analytics, feature flags, session replay, error tracking, and more.

It's a thin wrapper that delegates to the official PostHog SDKs on each target, so you get native batching, queueing, and session replay behind a single common API. The Web targets both delegate to `posthog-js`; Wasm support is experimental while Kotlin/Wasm remains in Beta.

Installation instructions, usage examples, and code snippets live in the official documentation so they stay up to date.

On iOS, the KMP SDK imports `posthog-ios` with Kotlin's SwiftPM integration. KMP applications should use [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html) and add the generated SwiftPM linkage package to their Xcode project; CocoaPods is not required. This currently requires Kotlin 2.4.20-Beta2 while SwiftPM import remains an Alpha Kotlin feature.

## Documentation

- [Kotlin Multiplatform library docs](https://posthog.com/docs/libraries/kmp)
- [Main PostHog docs](https://posthog.com/docs)

## Questions?

### [Check out our community page.](https://posthog.com/posts)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for local setup and test instructions, and [RELEASING.md](RELEASING.md) for the release process.

## License

[MIT](LICENSE)
