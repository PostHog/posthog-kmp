---
"posthog-kmp": major
---

- See the [0.12 to 1.0 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
- **Breaking:** build `PostHogConfig` and `CaptureOptions` with builder blocks, such as `PostHogConfig("phc_...") { debug = true }`, instead of constructors with named arguments, so later 1.x releases can add options without breaking compiled code; the config classes, `CaptureOptions` and `FeatureFlagResult` no longer have `copy()` or `componentN()`
- **Breaking:** set session replay and error tracking with `sessionReplay { }` and `errorTracking { }`, and rename `sessionRecording` to `sessionReplay` and `SessionRecordingConfig` to `SessionReplayConfig`, as in posthog-android and posthog-ios
- **Breaking:** rename the `additionalProperties` parameter of `captureException()` to `properties`, as in `capture()` and the native SDKs
- **Breaking:** remove the deprecated `PostHogConfig.apiKey` alias and the `PostHogConfig(apiKey = ...)` factories — use `projectToken`
- **Breaking:** turn `captureScreenViews` on by default, like posthog-android and posthog-ios, so Android, iOS and web send screen and page views unless you set it to `false`
- **Breaking:** stop throwing from `PostHogContext()` on Android — `PostHog.setup()` with it now leaves the SDK disabled and logs a warning, so pass `PostHogContext(application)`
- **Breaking:** make `PostHogContext.application` internal on Android
