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

## Docs

SDK usage examples and code snippets live in the [official documentation](https://posthog.com/docs/libraries/kmp), not the README, so they stay up to date. If your change affects the public API or behavior, update the docs in [PostHog/posthog.com](https://github.com/PostHog/posthog.com/blob/master/contents/docs/libraries/kmp/index.mdx).

## Releasing

Releases are semi-automatic: release change intents merged to `main` trigger the release pipeline, which waits for approval in the `#approvals-client-libraries` Slack channel. See [RELEASING.md](RELEASING.md) for details.
