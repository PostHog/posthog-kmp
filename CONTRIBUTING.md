# Contributing

Contributions are welcome!

## Development setup

```bash
git clone https://github.com/PostHog/posthog-kmp.git
cd posthog-kmp

# Build all targets
./gradlew build

# Run tests
./gradlew allTests
```

The `sample/` directory contains a Compose Multiplatform demo app that exercises the SDK.

### Apple targets

On an Apple Silicon Mac with Xcode installed:

```bash
./gradlew :posthog-kmp:macosArm64Test :posthog-kmp:iosSimulatorArm64Test
./scripts/test-apple-publication.sh macosArm64
./scripts/test-apple-publication.sh iosSimulatorArm64
```

The publication checks use an isolated local Maven repository and a separate
consumer with iOS and macOS targets. They compile shared Apple metadata and run
against the published artifacts, including their transitive SwiftPM dependency.

### JVM code coverage

```bash
./gradlew -I scripts/jvm-coverage.gradle :posthog-kmp:auditCoverage
```

JaCoCo XML and HTML reports are written to `posthog-kmp/build/reports/jacoco/auditCoverage/`.
This measures the SDK classes compiled for JVM (common, JVM-shared, and JVM sources),
not native dependencies, sample code, or the Android/iOS/JS/Wasm implementations.
Use the same command and class scope for before/after comparisons. Run `detekt`
separately from native build tasks to avoid Gradle's overlapping-output validation.

The sample iOS UI tests are manual, live-project smoke tests, not backend-delivery
assertions. They require an explicitly approved test project and `POSTHOG_API_KEY`;
do not use them as proof that events arrived at PostHog.

## Public API changes

Public API is hard to change once it ships, so agree on it before writing the implementation. Our [SDK guidelines](https://posthog.com/handbook/engineering/sdks/guidelines) explain how we design it.

This section is for external contributors. PostHog maintainers (members of the PostHog GitHub org) agree on API shape in the PR itself, so they don't need a separate issue.

- **Before you start:** if you need something the SDK doesn't support and it would add or change a public option, method, or type, open an issue describing your use case. Wait for a maintainer to agree on the API shape there before you implement it. Context is more useful to us than code at this stage.
- **Already specified?** If a published [sdk-spec](https://github.com/PostHog/sdk-specs) defines the API, that's the agreement, so you don't need an issue.
- **Already have a PR open?** Don't stop or rewrite it. Call out the public API change at the top of the PR description, and link or open an issue so we can discuss the shape there.
- Check first whether an existing option or hook, such as `beforeSend`, already covers the use case. We avoid offering two ways to do the same thing.
- If a reviewer suggests a different API on your PR, confirm it with them before re-implementing. Treat it as a question, not an instruction.

The module uses Kotlin's explicit API mode, so any declaration you mark `public` is public API.

## Docs

SDK usage examples and code snippets live in the [official documentation](https://posthog.com/docs/libraries/kmp), not the README, so they stay up to date. If your change affects the public API or behavior, update the docs in [PostHog/posthog.com](https://github.com/PostHog/posthog.com/blob/master/contents/docs/libraries/kmp/index.mdx).

## Releasing

Releases are semi-automatic: release change intents merged to `main` trigger the release pipeline, which waits for approval in the `#approvals-client-libraries` Slack channel. See [RELEASING.md](RELEASING.md) for details.
