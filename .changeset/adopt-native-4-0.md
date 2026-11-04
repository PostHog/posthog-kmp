---
"posthog-kmp": minor
---

- **Breaking:** require macOS 11 or later and upgrade to posthog-android 4.0, the JVM core 7.0 and posthog-ios 4.0
- **Breaking:** send events to capture v1 (`/i/v1/analytics/events`) on Android, JVM, iOS and macOS — make sure your reverse proxy or self-hosted instance serves that path
- **Breaking:** stop sending `$lib` and `$lib_version` set in event properties, `register()` or `beforeSend` — PostHog reports `posthog-kmp` and its version from the request instead
- **Breaking:** disable the SDK at setup when `projectToken` is a personal API key (`phx_...`) on Android, JVM, iOS and macOS
- **Breaking:** return a `null` `FeatureFlagResult.payload` for a payload that isn't valid JSON on Android, JVM, iOS and macOS
- **Breaking:** let `register()` super properties override properties the SDK sets, such as `$os_name`, on Android, JVM, iOS and macOS
- **Breaking:** change `$locale` to the device's preferred language and region on Android, iOS and macOS
