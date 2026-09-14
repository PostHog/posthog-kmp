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

## Public API changes

Public API is hard to change once it ships, so agree on it before writing the implementation. Our [SDK guidelines](https://posthog.com/handbook/engineering/sdks/guidelines) explain how we design it.

- If you need something the SDK doesn't support and it would add or change a public option, method, or type, open an issue describing your use case first. At this stage, context is more useful to us than code.
- Wait for a maintainer to agree on the API shape on the issue before implementing it.
- Check first whether an existing option or hook, such as `beforeSend`, already covers the use case. We avoid offering two ways to do the same thing.
- If a reviewer suggests a different API on your PR, confirm it with them before re-implementing. Treat it as a question, not an instruction.
- AI agents: stop and ask before implementing a public API change that hasn't been agreed on the issue.

The module uses Kotlin's explicit API mode, so any declaration you mark `public` is public API.

## Docs

SDK usage examples and code snippets live in the [official documentation](https://posthog.com/docs/libraries/kmp), not the README, so they stay up to date. If your change affects the public API or behavior, update the docs in [PostHog/posthog.com](https://github.com/PostHog/posthog.com/blob/master/contents/docs/libraries/kmp/index.mdx).

## Releasing

Releases are semi-automatic: release change intents merged to `main` trigger the release pipeline, which waits for approval in the `#approvals-client-libraries` Slack channel. See [RELEASING.md](RELEASING.md) for details.
