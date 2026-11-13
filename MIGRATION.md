# Migrating posthog-kmp

## 0.10 to 0.11

0.11 moves Android, JVM, iOS and macOS to the 4.0 native SDKs: posthog-android 4.0, the JVM core `com.posthog:posthog` 7.0 and posthog-ios 4.0. JavaScript and Wasm targets stay on posthog-js; of the breaking changes below, only the `SessionRecordingConfig` changes in [Session replay](#session-replay), which are compile errors in common code, and [Feature flags](#feature-flags) reach them.

<!-- TODO(native-4.0-links): these tags don't exist until posthog-android 4.0.0 and posthog-ios 4.0.0 are released; check both links resolve before merging. -->
The native SDKs list every change in their own guides: [posthog-android](https://github.com/PostHog/posthog-android/blob/android-v4.0.0/MIGRATION.md) and [posthog-ios](https://github.com/PostHog/posthog-ios/blob/4.0.0/MIGRATION.md). This guide covers what reaches a KMP app. If your app also calls posthog-android, posthog-ios or the JVM core `com.posthog:posthog` directly, that code moves to 4.0 (core 7.0) too, so follow those guides for it.

### Checklist

- [ ] Recompile any library built against posthog-kmp 0.10. Its JVM and Android bytecode no longer links, because some signatures changed.
- [ ] If your Android or JVM build also uses `posthog-android-surveys-compose` or `posthog-server`, see [Other PostHog artifacts](#other-posthog-artifacts).
- [ ] Build iOS and macOS apps with Xcode 26 or later, and set the macOS deployment target to 11.0 or later.
- [ ] Build your Xcode app once and commit the updated `KotlinMultiplatformLinkedPackage`, which now pins posthog-ios 4.0. If that build stops with `Synthetic project state updated`, run it again.
- [ ] Remove `screenshot` from `SessionRecordingConfig`, and mask custom-drawn content before you ship.
- [ ] If you record session replay on iOS with Compose Multiplatform, mask the Compose view first: its text fields aren't masked (see [Session replay](#session-replay)).
- [ ] If you send events through a reverse proxy or a self-hosted instance, serve `/i/v1/analytics/events`, forward its `Authorization`, `PostHog-*`, `Content-Type`, `Content-Encoding` and `User-Agent` headers, and pass the response body through unchanged.
- [ ] Update `reloadFeatureFlags` callbacks that are function references or `() -> Unit` values to take the new `PostHogFeatureFlagsLoaded` parameter.
- [ ] Check that `projectToken` is your project token (`phc_...`), not a personal API key (`phx_...`).
- [ ] If you call `register("\$process_person_profile", false)`, remove it and use `personProfiles` instead (see [Event changes](#event-changes)).
- [ ] Update `beforeSend` callbacks that read or set the `$process_person_profile` property to use `PostHogEvent.options["process_person_profile"]` (see [Event changes](#event-changes)).
- [ ] If you call `captureAi()` through a reverse proxy or a self-hosted instance, serve `/i/v1/ai/events` too (see [Event options](#event-options)).
- [ ] Update insights, cohorts and alerts that read the properties listed under [Event changes](#event-changes).

### Build requirements

posthog-ios 4.0 uses `swift-tools-version` 6.2, so building the iOS and macOS targets needs Xcode 26. The minimum macOS version is now 11.0; the minimum iOS version stays 15.0.

posthog-ios 4.0 is published to Swift Package Manager only. KMP already imports it through SwiftPM, so you don't need to change how you add the SDK. The generated `KotlinMultiplatformLinkedPackage` in your Xcode project updates to posthog-ios 4.0 on the next build; commit the change.

### Binary compatibility

Code compiled against 0.10 has to be recompiled against 0.11, including libraries you publish that depend on posthog-kmp. Some JVM signatures changed, so 0.10 bytecode fails at runtime with `NoSuchMethodError`:

- `SessionRecordingConfig` lost `screenshot` and stores the throttle delay in `throttleDelayMs` instead of `debouncerDelayMs`, which changes its constructor, `copy()` and `component7()` onward. `debouncerDelayMs` is still readable as a deprecated alias. `component6()` keeps its signature but now returns `captureLogcat` instead of `screenshot`, so 0.10 bytecode that calls it gets a different value without an error.
- `reloadFeatureFlags()` takes a `(PostHogFeatureFlagsLoaded) -> Unit` callback instead of `() -> Unit`.
- `CaptureOptions` gained `eventOptions` and `PostHogEvent` gained `options`, which changes their constructors and `copy()`. A `beforeSend` callback compiled against 0.10 that calls `event.copy(...)` fails.
- `screen()`, `identify()`, `alias()`, `group()`, `setPersonProperties()` and `captureException()` take a trailing `eventOptions` parameter. Java callers have no overload without it, so Java source that calls them, or `new CaptureOptions(groups, timestamp)`, has to pass the new argument.

`PostHogConfig` keeps its 0.10 constructor as a public overload and its 0.10 `copy()` as a hidden overload, so code that only builds a `PostHogConfig` still links.

#### Other PostHog artifacts

The JVM core `com.posthog:posthog` 7.0 isn't binary compatible with artifacts built against core 6.x, and Gradle resolves one version for the whole build. If your Android or JVM build also pulls in:

- `com.posthog:posthog-android-surveys-compose`, keep its version and declare `com.posthog:posthog-android` 4.0.0 or later, so Gradle doesn't resolve the 3.x version it depends on.
- `com.posthog:posthog-server`, use 2.25.0 or later. Older versions fail at runtime against core 7.0 with errors such as `NoSuchMethodError`.

### Session replay

Session replay always records masked screenshots. Wireframe recording and `SessionRecordingConfig.screenshot` are gone:

```kotlin
// 0.10
SessionRecordingConfig(screenshot = true, maskAllImages = true)

// 0.11
SessionRecordingConfig(maskAllImages = true)
```

Apps that kept the default `screenshot = false` now upload screenshots. Text inputs and images stay masked by default, but custom-drawn content such as maps, video or canvas drawing is captured as it appears. Mask it with the native SDKs' masking APIs, or turn replay off for those screens.

On Android, Compose screens, including Compose Multiplatform UI, are now recorded; wireframe captured them as empty boxes. That can count against your replay quota and may need a consent or privacy disclosure update.

On iOS, Compose Multiplatform screens are now recorded too; 0.10 recorded no frames for them. posthog-ios finds what to mask through UIKit views, and Compose draws its whole UI into one view, so `maskAllTextInputs` and `maskAllImages` don't mask the text fields, text or images inside it: what users type is recorded. Until that's fixed, mask the whole Compose view where you create it, or enable `sessionRecording` on Android only:

```swift
let controller = MainViewControllerKt.MainViewController()
controller.view.accessibilityIdentifier = "ph-no-capture"
```

`debouncerDelayMs` is deprecated in favor of `throttleDelayMs`, which now applies on iOS as well as Android. The constructor still accepts `debouncerDelayMs` as a named argument, but `copy(debouncerDelayMs = ...)` no longer compiles: use `copy(throttleDelayMs = ...)`.

Because `screenshot` is gone, positional arguments and destructuring shift: `throttleDelayMs` (a `Long`) now sits where `captureLogcat` was. Rewrite positional calls with named arguments. Java callers of `new SessionRecordingConfig(...)` drop the `screenshot` argument and pass `throttleDelayMs`; `debouncerDelayMs` is only reachable from Java through `SessionRecordingConfig.Companion.invoke(...)`.

### Capture v1

Android, JVM, iOS and macOS send events to `/i/v1/analytics/events` with the project token in an `Authorization: Bearer` header, instead of `/batch`. PostHog Cloud already serves this path. A reverse proxy or self-hosted instance must serve it too. A reverse proxy must:

- Pass these request headers through unchanged: `Authorization` (`Bearer <project token>`), `PostHog-Sdk-Info`, `PostHog-Attempt`, `PostHog-Request-Id`, `PostHog-Request-Timestamp`, `Content-Type`, `Content-Encoding` and `User-Agent`. PostHog answers 401 without `Authorization` and 400 without the others, and the events are dropped.
- Pass PostHog's response body through unchanged. A 2xx response whose body isn't PostHog's `{"results": {...}}` drops the events instead of retrying them.

A proxy set up from PostHog's [proxy guides](https://posthog.com/docs/advanced/proxy) before 29 October 2026 strips `Authorization`, so PostHog answers 401 and the events are dropped; update it from the current guide. PostHog's managed reverse proxy needs no change.

Until your host serves the path, a 404 is retried with no limit, about every 30 seconds, and events stay queued only until a full queue evicts them (`maxQueueSize`, 1000 by default), oldest first. Any other error status that isn't retried, such as 401, 403 or 405, drops the events.

Capture requests follow only 307 and 308 redirects to the same origin as `host`, and drop events on any other redirect.

Retries also changed: 3xx responses are no longer retried, `Retry-After` is capped at 30 seconds, the wait between failed sends doubles with jitter, and a request carries at most about 5 MiB. See the Retries sections of the native guides.

Events queued on disk by 0.10 are sent through capture v1 after the update.

### Event options

Settings that tell PostHog how to process an event are now sent as event options, apart from its properties. Set them with `CaptureOptions.eventOptions`, the `eventOptions` parameter of `screen()`, `identify()`, `alias()`, `group()`, `setPersonProperties()` and `captureException()`, or for every later event with `registerOptions()`. `PostHogEventOptions` has the keys:

| Option | Replaces the property |
|---|---|
| `PostHogEventOptions.PROCESS_PERSON_PROFILE` (`process_person_profile`) | `$process_person_profile` |
| `PostHogEventOptions.COOKIELESS_MODE` (`cookieless_mode`) | `$cookieless_mode` |
| `PostHogEventOptions.DISABLE_SKEW_CORRECTION` (`disable_skew_correction`) | `$ignore_sent_at` |
| `PostHogEventOptions.PRODUCT_TOUR_ID` (`product_tour_id`) | `$product_tour_id` |

The properties still work, and an option wins over its property. Options are sent as given, so a key not in the table also works.

```kotlin
// Before
PostHog.capture("checkout", mapOf("\$cookieless_mode" to true))
PostHog.register("\$cookieless_mode", true)

// After
PostHog.capture("checkout", options = CaptureOptions(eventOptions = mapOf(PostHogEventOptions.COOKIELESS_MODE to true)))
PostHog.registerOptions(mapOf(PostHogEventOptions.COOKIELESS_MODE to true))
```

On JavaScript and Wasm, posthog-js has no options, so the keys in the table are sent as their properties on `capture()`, `screen()` and `captureException()` events, and other keys are dropped. `registerOptions()` registers those keys as their properties there and drops other keys.

`PostHog.captureAi()` sends AI events, such as `$ai_generation`, to PostHog's AI endpoint, `/i/v1/ai/events`, on Android, JVM, iOS and macOS. A reverse proxy or self-hosted instance has to serve that path too, with the same headers and response handling as [Capture v1](#capture-v1). `PostHogConfig.captureAiCompression`, `captureAiMaxQueueSize`, `captureAiTimeout` and `captureAiMaxEventBytes` configure those requests.

### Setup

`setup()` leaves the SDK disabled when `projectToken` is a personal API key (`phx_...`), the same as for an empty token, and prints a warning even when `debug` is off.

### Feature flags

On Android, JVM, iOS and macOS, `FeatureFlagResult.payload` is `null` for a payload that isn't valid JSON, such as plain text or an empty string, instead of the raw string. A JSON string payload such as `"text"` still returns `"text"`. On Android and JVM, a JSON `null` payload is `null` instead of the string `"null"`. JavaScript and Wasm still return the raw string.

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

A flag whose variant is an empty string is disabled on every platform: `isFeatureEnabled()` returns `false` and `FeatureFlagResult.enabled` is `false`, including in the results of `getAllFeatureFlags()`. iOS and macOS used to report it as enabled in `isFeatureEnabled()`, and JavaScript and Wasm in `FeatureFlagResult.enabled`. On Android, JVM, iOS and macOS, captured events no longer list it in `$active_feature_flags`; JavaScript and Wasm already left it out. `getFeatureFlag()` and the event's `$feature/<key>` property still return `""`.

To react to every flag load rather than one reload, use the new `PostHog.onFeatureFlags()`.

### Event changes

| Change | Platforms | What to do |
|---|---|---|
| Events with an empty name are dropped, including one a `beforeSend` callback renames to `""`. Events whose distinct ID a `beforeSend` callback sets to an empty or whitespace-only string are dropped too. PostHog also drops events whose name or distinct ID is over 200 bytes. | Android, JVM, iOS, macOS | Give every event a name, and don't blank the distinct ID in `beforeSend`. |
| When the SDK doesn't process person profiles for an event (an anonymous user's event with the default `personProfiles`), it now marks that with the `process_person_profile` event option instead of a `$process_person_profile = false` property. `beforeSend` sees it in `PostHogEvent.options["process_person_profile"]`. | Android, JVM, iOS, macOS | To process person profiles for an event, set `process_person_profile` in `PostHogEvent.options` in `beforeSend`, pass `CaptureOptions(eventOptions = mapOf(PostHogEventOptions.PROCESS_PERSON_PROFILE to true))` to `capture()`, call `identify()`, or set `personProfiles = PersonProfiles.ALWAYS`. Update callbacks that read the property to read `options["process_person_profile"]`. |
| `$feature_flag_called` for a missing flag leaves out `$feature_flag_response` instead of sending `""` or `false`. | Android, JVM, iOS, macOS | Filter on `$feature_flag_response` *is not set* instead. |
| A `null` value inside a nested map is left out instead of sent as `null`. `$set` and `$set_once` still send `null` values. | iOS, macOS | Don't rely on `null` entries in nested maps. |
| A `null` element in a list is sent as `null` at its position instead of being removed, which used to shift later elements. | Android, JVM | Update filters that index into list properties. |
| `$sdk_debug_replay_capture_mode` is always `screenshot`. | Android, iOS | Update filters on `wireframe`. |
| `Deep Link Opened` leaves out query parameters whose name starts with `$`. | Android | Rename any `$`-prefixed parameter you read from the event. |
| Super properties from `register()` override properties the SDK sets, such as `$os_name`. A registered `$session_id` replaces the SDK's session ID, which can detach events from their session replay. | Android, JVM, iOS, macOS | Don't register `$`-prefixed SDK properties unless you mean to replace them. |
| A `$process_person_profile = false` registered with `register()`, or a `process_person_profile = false` registered with `registerOptions()`, now applies to every event except the ones `identify()`, `alias()`, `group()` and `setPersonProperties()` send. A value passed on those calls, or set in `beforeSend`, still applies. In 0.10, the SDK replaced a registered value with its own, so it had no effect. Events after `identify()` that get the registered value are still linked to the person and billed as identified events. | Android, JVM, iOS, macOS | Remove the registered value. To skip person profiles for anonymous users, rely on `personProfiles = PersonProfiles.IDENTIFIED_ONLY`, the default, which still lets `identify()` create the person. To create no person profiles at all, set `personProfiles = PersonProfiles.NEVER`. |
| `$locale` is the device's preferred language with its region, such as `de-CH`. On Android it's left out when the device reports no language, and the legacy codes `iw`, `in` and `ji` become `he`, `id` and `yi`. | Android, iOS, macOS | Filter on the new `$app_language` for the language the app shows, and update filters that match the legacy codes. |
| `$locale` is a BCP 47 tag that keeps the script, such as `zh-Hant-TW`, and is `en` rather than `en-` when the locale has no region. | JVM | Update filters that match the old form. |
| `Application Installed`, `Application Updated` and `Application Opened` no longer have `version` and `build`. | iOS, macOS | Use `$app_version` and `$app_build`. |
| New `$app_language` property with the language the app displays. Set it yourself with `PostHog.setAppLanguage()` on Android, JVM, iOS and macOS. | Android 13+, iOS, macOS | See [App language](#app-language) if you translate with Compose Multiplatform resources. |

### App language

The native SDKs detect `$app_language` from the languages your app declares: `localeConfig` on Android 13+, and the bundle's localizations on iOS and macOS. Compose Multiplatform resources don't declare either, so:

- **iOS and macOS** report your development language, such as `en`, whatever language the app shows. List the languages you translate to under `CFBundleLocalizations` in the app's `Info.plist`. This also adds your app to the per-app language list in iOS Settings.
- **Android** sends no `$app_language` unless the app declares a `localeConfig` or the user picks a per-app language.

If your app picks its language some other way, such as an in-app setting, call `PostHog.setAppLanguage()` at startup and whenever the language changes:

```kotlin
PostHog.setAppLanguage("pt-BR")
PostHog.setAppLanguage(null) // back to the detected language
```

<!-- TODO(app-language-before-setup): remove the next sentence once Android and JVM keep a language set before setup(). -->
On Android and JVM, call `setAppLanguage()` after `PostHog.setup()`: a call before setup is dropped. The value is kept in memory only and survives `reset()`. A `$app_language` super property set with `register()` takes precedence. JVM sends `$app_language` only after you call `setAppLanguage()`. JavaScript and Wasm don't send it yet, because posthog-js doesn't support it, so `setAppLanguage()` does nothing there; to send it now, call `PostHog.register("\$app_language", "pt-BR")`.

### New in 0.11

These additions work on every platform, including JavaScript and Wasm, except `setAppLanguage()`, which does nothing on JavaScript and Wasm.

- `PostHog.setPersonPropertiesForFlags()` and `PostHog.setGroupPropertiesForFlags()` evaluate feature flags with properties you just set, before PostHog processes them. Clear them with `resetPersonPropertiesForFlags()` and `resetGroupPropertiesForFlags()`.
- `PostHogConfig.evaluationContexts` limits feature flag evaluation to the flags tagged with at least one of those contexts, plus flags with no tags.
- `PostHog.onFeatureFlags()` calls you back on every feature flag load.
- `PostHog.setAppLanguage()` sets `$app_language`; see [App language](#app-language).

```kotlin
val config = PostHogConfig(projectToken = "phc_...", evaluationContexts = listOf("checkout"))
PostHog.setPersonPropertiesForFlags(mapOf("plan" to "pro"))
PostHog.setGroupPropertiesForFlags("company", mapOf("plan" to "enterprise"))
```
