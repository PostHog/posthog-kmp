---
"posthog-kmp": major
---

- **Breaking:** remove the deprecated `PostHogConfig.apiKey` alias and the `PostHogConfig(apiKey = ...)` factories — use `projectToken`
- **Breaking:** remove the `PostHogConfig` constructor and `copy()` overloads kept for earlier versions' argument lists, so Java callers pass every argument
