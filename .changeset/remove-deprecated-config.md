---
"posthog-kmp": major
---

- See the [0.12 to 1.0 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
- **Breaking:** remove the deprecated `PostHogConfig.apiKey` alias and the `PostHogConfig(apiKey = ...)` factories — use `projectToken`
- **Breaking:** remove the `PostHogConfig` constructor and `copy()` overloads kept for earlier versions' argument lists, so Java callers pass every argument and libraries built against them need recompiling
- **Breaking:** turn `captureScreenViews` on by default, like posthog-android and posthog-ios, so Android, iOS and web send screen and page views unless you set it to `false`
