# Migrating posthog-kmp

## 0.10 to 0.11

0.11 moves Android, JVM, iOS and macOS to the 4.0 native SDKs: posthog-android 4.0, the JVM core `com.posthog:posthog` 7.0 and posthog-ios 4.0. JavaScript and Wasm targets don't change.

The native SDKs list every change in their own guides: [posthog-android](https://github.com/PostHog/posthog-android/blob/main/docs/migration-3.x-to-4.0.md) and [posthog-ios](https://github.com/PostHog/posthog-ios/blob/main/MIGRATION.md). This guide covers what reaches a KMP app.

### Checklist

- [ ] Build iOS and macOS apps with Xcode 26 or later, and set the macOS deployment target to 11.0 or later.
- [ ] Build your Xcode app once and commit the updated `KotlinMultiplatformLinkedPackage`, which now pins posthog-ios 4.0. If that build stops with `Synthetic project state updated`, run it again.
- [ ] Remove `screenshot` from `SessionRecordingConfig`, and mask custom-drawn content before you ship.
- [ ] If you send events through a reverse proxy or a self-hosted instance, serve `/i/v1/analytics/events` and forward its `Authorization` and `Content-Encoding` headers.
- [ ] Check that `projectToken` is your project token (`phc_...`), not a personal API key (`phx_...`).
- [ ] Update insights, cohorts and alerts that read the properties listed under [Event changes](#event-changes).

### Build requirements

posthog-ios 4.0 uses `swift-tools-version` 6.2, so building the iOS and macOS targets needs Xcode 26. The minimum macOS version is now 11.0; the minimum iOS version stays 15.0.

posthog-ios 4.0 is published to Swift Package Manager only. KMP already imports it through SwiftPM, so you don't need to change how you add the SDK. The generated `KotlinMultiplatformLinkedPackage` in your Xcode project updates to posthog-ios 4.0 on the next build; commit the change.

### Session replay

Session replay always records masked screenshots. Wireframe recording and `SessionRecordingConfig.screenshot` are gone:

```kotlin
// 0.10
SessionRecordingConfig(screenshot = true, maskAllImages = true)

// 0.11
SessionRecordingConfig(maskAllImages = true)
```

Apps that kept the default `screenshot = false` now upload screenshots. Text inputs and images stay masked by default, but custom-drawn content such as maps, video or canvas drawing is captured as it appears. Mask it with the native SDKs' masking APIs, or turn replay off for those screens.

### Capture v1

Android, JVM, iOS and macOS send events to `/i/v1/analytics/events` with the project token in an `Authorization: Bearer` header, instead of `/batch`. PostHog Cloud already serves this path. A reverse proxy or self-hosted instance must forward it too, and pass `Authorization` and `Content-Encoding` through unchanged. A proxy set up from PostHog's [proxy guides](https://posthog.com/docs/advanced/proxy) before 29 October 2026 strips `Authorization`, so PostHog answers 401 and the events are dropped; update it from the current guide. PostHog's managed reverse proxy needs no change. Until it does, a 404 is retried with no limit, about every 30 seconds, and events stay queued only until a full queue evicts them (`maxQueueSize`, 1000 by default), oldest first. Any other error status that isn't retried, such as 401, 403 or 405, drops the events.

Capture requests follow only 307 and 308 redirects to the same origin as `host`, and drop events on any other redirect.

Events queued on disk by 0.10 are sent through capture v1 after the update.

### Setup

`setup()` leaves the SDK disabled when `projectToken` is a personal API key (`phx_...`), the same as for an empty token, and prints a warning even when `debug` is off.

### Feature flags

`FeatureFlagResult.payload` is `null` for a payload that isn't valid JSON, such as plain text or an empty string, instead of the raw string. A JSON string payload such as `"text"` still returns `"text"`.

The `reloadFeatureFlags()` callback receives a `PostHogFeatureFlagsLoaded` with the enabled flags, their values, and `errorsLoading`, which is `true` when the reload failed. A lambda that ignores its parameter still compiles; a function reference or a `() -> Unit` value needs the parameter:

```kotlin
// 0.10
PostHog.reloadFeatureFlags { showCheckout() }

// 0.11
PostHog.reloadFeatureFlags { loaded ->
    if (!loaded.errorsLoading) showCheckout()
}
```

Before `setup()`, the callback runs right away with no flags and `errorsLoading = true` on every platform. On Android and JVM it used to never run.

A flag whose variant is an empty string is disabled on every platform: `isFeatureEnabled()` returns `false` and `FeatureFlagResult.enabled` is `false`. iOS and macOS used to report it as enabled in `isFeatureEnabled()`, and JavaScript and Wasm in `FeatureFlagResult.enabled`.

To react to every flag load rather than one reload, use the new `PostHog.onFeatureFlags()`.

### Event changes

| Change | Platforms | What to do |
|---|---|---|
| Events with an empty name are dropped, including one a `beforeSend` callback renames to `""`. PostHog also drops events whose name or distinct ID is over 200 bytes. | Android, JVM, iOS, macOS | Give every event a name. |
| Super properties from `register()` override properties the SDK sets, such as `$os_name`. | Android, JVM, iOS, macOS | Don't register `$`-prefixed SDK properties unless you mean to replace them. |
| `$locale` is the device's preferred language with its region, such as `de-CH`. | Android, iOS, macOS | Filter on the new `$app_language` for the language the app shows. |
| `Application Installed`, `Application Updated` and `Application Opened` no longer have `version` and `build`. | iOS, macOS | Use `$app_version` and `$app_build`. |
| New `$app_language` property with the language the app displays. | Android 13+, iOS, macOS | None. |
