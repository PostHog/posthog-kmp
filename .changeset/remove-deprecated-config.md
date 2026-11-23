---
"posthog-kmp": major
---

- See the [0.12 to 1.0 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
- **Breaking:** make `PostHogConfig`, `SessionRecordingConfig` and `ErrorTrackingConfig` regular classes instead of data classes, so they no longer have `copy()`, `componentN()` or value-based `equals()` — build a new config instead of calling `copy()`
- **Breaking:** remove the deprecated `PostHogConfig.apiKey` alias and the `PostHogConfig(apiKey = ...)` factories — use `projectToken`
- **Breaking:** turn `captureScreenViews` on by default, like posthog-android and posthog-ios, so Android, iOS and web send screen and page views unless you set it to `false`
- **Breaking:** stop throwing from `PostHogContext()` on Android — `PostHog.setup()` with it now leaves the SDK disabled and logs a warning, so pass `PostHogContext(application)`
- **Breaking:** make `PostHogContext.application` internal on Android
- Let Java pass any leading part of the `PostHogConfig`, `SessionRecordingConfig` and `ErrorTrackingConfig` constructor arguments, including the 0.10 and 0.11 argument lists, so new options no longer break Java callers or compiled libraries
