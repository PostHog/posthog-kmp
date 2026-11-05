---
"posthog-kmp": minor
---

- See the [0.10 to 0.11 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
- **Breaking:** upgrade to posthog-android 4.0, the JVM core 7.0 and posthog-ios 4.0
- **Breaking:** require Xcode 26 or later to build iOS and macOS apps, and macOS 11 or later
- **Breaking:** send events to capture v1 (`/i/v1/analytics/events`) on Android, JVM, iOS and macOS — make sure your reverse proxy or self-hosted instance serves that path; only 307 and 308 redirects to the same origin are followed
- **Breaking:** disable the SDK at setup when `projectToken` is a personal API key (`phx_...`) on Android, JVM, iOS and macOS
- **Breaking:** drop events with an empty name, including one `beforeSend` renames to `""`, and let PostHog drop events whose name or distinct ID is over 200 bytes, on Android, JVM, iOS and macOS
- **Breaking:** return a `null` `FeatureFlagResult.payload` for a payload that isn't valid JSON on Android, JVM, iOS and macOS
- **Breaking:** let `register()` super properties override properties the SDK sets, such as `$os_name`, on Android, JVM, iOS and macOS
- **Breaking:** change `$locale` to the device's preferred language and region on Android, iOS and macOS
- **Breaking:** remove the `version` and `build` properties from `Application Installed`, `Application Updated` and `Application Opened` on iOS and macOS — use `$app_version` and `$app_build`
- Add the `$app_language` property with the language the app displays on Android 13+, iOS and macOS
