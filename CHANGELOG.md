# posthog-kmp

## 1.1.0

### Minor Changes

- Upgrade the Android SDK to 4.1.0, the JVM core to 7.1.0, and the iOS SDK to 4.1.0. These versions add session replay v2 trigger groups.

## 1.0.0

### Major Changes

- - See the [0.12 to 1.0 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
  - **Breaking:** build `PostHogConfig` and `CaptureOptions` with builder blocks, such as `PostHogConfig("phc_...") { debug = true }`, instead of constructors with named arguments, so later 1.x releases can add options without breaking compiled code; the config classes, `CaptureOptions` and `FeatureFlagResult` no longer have `copy()` or `componentN()`
  - **Breaking:** copy `PostHogEvent` in `beforeSend` with `event.copy { properties = ... }` instead of `event.copy(properties = ...)`, so later releases can add event fields; destructuring an event no longer compiles
  - **Breaking:** make the `FeatureFlagResult` constructor internal
  - **Breaking:** set session replay and error tracking with `sessionReplay { }` and `errorTracking { }`, and rename `sessionRecording` to `sessionReplay` and `SessionRecordingConfig` to `SessionReplayConfig`, as in posthog-android and posthog-ios
  - **Breaking:** rename the `additionalProperties` parameter of `captureException()` to `properties`, as in `capture()` and the native SDKs
  - **Breaking:** remove the deprecated `PostHogConfig.apiKey` alias and the `PostHogConfig(apiKey = ...)` factories — use `projectToken`
  - **Breaking:** turn `captureScreenViews` on by default, like posthog-android and posthog-ios, so Android, iOS and web send screen and page views unless you set it to `false`
  - **Breaking:** stop throwing from `PostHogContext()` on Android — `PostHog.setup()` with it now leaves the SDK disabled and logs a warning, so pass `PostHogContext(application)`
  - **Breaking:** make `PostHogContext.application` internal on Android

## 0.12.0

### Minor Changes

- - See the [0.11 to 0.12 migration guide](https://github.com/PostHog/posthog-kmp/blob/main/MIGRATION.md) for upgrade steps
  - **Breaking:** upgrade to posthog-android 4.0, the JVM core 7.0 and posthog-ios 4.0
  - **Breaking:** require Xcode 26 or later to build iOS and macOS apps, and macOS 12 or later
  - **Breaking:** require recompiling libraries built against posthog-kmp 0.11 or earlier, which fail on Android and JVM with `NoSuchMethodError`
  - **Breaking:** require `posthog-server` 2.26.0 or later, and `posthog-android` 4.0.0 declared alongside `posthog-android-surveys-compose`, in the same Android or JVM build
  - **Breaking:** send events to capture v1 (`/i/v1/analytics/events`) on Android, JVM, iOS and macOS — make sure your reverse proxy or self-hosted instance serves that path; only 307 and 308 redirects to the same origin are followed
  - **Breaking:** retry a capture 404 until the host serves capture v1, keeping events only until a full queue evicts them, stop retrying 3xx responses, and cap a request at about 5 MiB, on Android, JVM, iOS and macOS
  - **Breaking:** disable the SDK at setup when `projectToken` is a personal API key (`phx_...`) on Android, JVM, iOS and macOS
  - **Breaking:** drop events with an empty name or with a distinct ID that `beforeSend` blanks, and let PostHog drop events whose name or distinct ID is over 200 bytes, on Android, JVM, iOS and macOS
  - **Breaking:** mark personless events with a capture option instead of a `$process_person_profile` property, on Android, JVM, iOS and macOS — `beforeSend` reads and changes it through `PostHogEvent.options`
  - **Breaking:** leave out `null` values in nested maps on iOS and macOS, and keep `null` list elements in place instead of removing them on Android and JVM
  - **Breaking:** always send `$sdk_debug_replay_capture_mode` as `screenshot` on Android and iOS
  - **Breaking:** leave out query parameters whose name starts with `$` from `Deep Link Opened` on Android
  - **Breaking:** leave out `$feature_flag_response` on `$feature_flag_called` for a missing flag on Android, JVM, iOS and macOS — filter on *is not set* instead of `""` or `false`
  - **Breaking:** return a `null` `FeatureFlagResult.payload` for a payload that isn't valid JSON on Android, JVM, iOS and macOS
  - **Breaking:** let `register()` super properties override properties the SDK sets, such as `$os_name`, on Android, JVM, iOS and macOS
  - **Breaking:** apply a registered `$process_person_profile = false` to every event except those `identify()`, `alias()`, `group()` and `setPersonProperties()` send, on Android, JVM, iOS and macOS — use `personProfiles` instead
  - **Breaking:** change `$locale` to the device's preferred language and region on Android, iOS and macOS, and to a BCP 47 tag that keeps the script, such as `zh-Hant-TW`, on JVM
  - **Breaking:** remove the `version` and `build` properties from `Application Installed`, `Application Updated` and `Application Opened` on iOS and macOS — use `$app_version` and `$app_build`
  - Add the `$app_language` property with the language the app displays on Android 13+, iOS and macOS, and `PostHog.setAppLanguage()` to set it on every platform except JavaScript and Wasm

- - **Breaking:** remove `SessionRecordingConfig.screenshot`, because posthog-android 4.0 and posthog-ios 4.0 removed wireframe replay — apps that kept the default `false` now record masked screenshots, including Compose screens on Android, so mask custom-drawn content such as maps or video, or turn replay off
  - **Breaking:** record Compose Multiplatform screens on iOS, which 0.11 didn't, fully masked while `maskAllTextInputs` or `maskAllImages` is on
  - **Breaking:** remove the JVM `SessionRecordingConfig` constructor and `copy` overloads that took `screenshot`

- - **Breaking:** pass `PostHogFeatureFlagsLoaded` with the loaded flags and `errorsLoading` to the `reloadFeatureFlags()` callback, and call it with `errorsLoading = true` before `setup()` on every platform
  - **Breaking:** treat a flag whose variant is an empty string as disabled in `isFeatureEnabled()`, `FeatureFlagResult.enabled`, `getAllFeatureFlags()` and `$active_feature_flags` on every platform
  - Add `PostHog.onFeatureFlags()` to listen for feature flag loads, returning a `PostHogFeatureFlagsSubscription`
  - Add `setPersonPropertiesForFlags()`, `resetPersonPropertiesForFlags()`, `setGroupPropertiesForFlags()` and `resetGroupPropertiesForFlags()`
  - Add `PostHogConfig.evaluationContexts` to evaluate only the feature flags tagged with those contexts

- - Add `SessionRecordingConfig.iosCaptureMethod` to choose how iOS captures session replay screenshots. It defaults to `STANDARD`, like posthog-ios; the experimental `GPU` takes less main-thread time but doesn't capture web view pages and records Metal content, including Compose Multiplatform screens, as a placeholder

- - **Breaking:** replace `SessionRecordingConfig.debouncerDelayMs` with `throttleDelayMs`, the minimum delay between session replay snapshots, which now applies on iOS as well as Android

- - Add event options, set per event as a map through `CaptureOptions.eventOptions` and a new `eventOptions` parameter on `screen()`, `identify()`, `alias()`, `group()`, `setPersonProperties()` and `captureException()` — on JavaScript and Wasm they're sent as their legacy `$` properties on `capture()`, `screen()` and `captureException()` and dropped by the other calls
  - Add `PostHogEventOptions` with the option keys `PROCESS_PERSON_PROFILE`, `COOKIELESS_MODE`, `DISABLE_SKEW_CORRECTION` and `PRODUCT_TOUR_ID`; other keys are sent as given on Android, JVM, iOS and macOS
  - Add `PostHog.registerOptions()` and `PostHog.unregisterOptions()` to send event options with every later event
  - Add `PostHogEvent.options` so `beforeSend` callbacks can read and change an event's options on Android, JVM, iOS and macOS
  - Add `PostHog.captureAi()` to send AI observability events, such as `$ai_generation`, to PostHog's AI endpoint on Android, JVM, iOS and macOS, returning the event's UUID; it sends nothing on JavaScript and Wasm
  - Add `PostHogConfig.captureAiCompression`, `captureAiMaxQueueSize`, `captureAiTimeout` and `captureAiMaxEventBytes` to configure `captureAi()` requests on Android, JVM, iOS and macOS
  - Name the event and the exception type in the debug log when a `beforeSend` callback throws

## 0.11.0

### Minor Changes

- Add `surveys` to `PostHogConfig` to turn off surveys on iOS, Android and web. It defaults to `true`, so iOS and web don't change. On Android it now turns on the native surveys integration, which shows surveys only when the app also depends on `com.posthog:posthog-android-surveys-compose`; set it to `false` to keep the previous behavior.

## 0.10.0

### Minor Changes

- Expose the experimental Android screenshot mask-alignment verification option through SessionRecordingConfig.

## 0.9.1

### Patch Changes

- Upgrade the Android SDK to 3.72.9 and the JVM core to 6.47.5, which pull in OkHttp 4.12.1 to block the compromised eMudhra intermediate certificates.

## 0.9.0

### Minor Changes

- Expose Android push notification subscription and open capture options in PostHogConfig.

## 0.8.0

### Minor Changes

- **Breaking:** rename `PostHogConfig.apiKey` to `projectToken`; `apiKey` still compiles with a deprecation warning, but `copy(apiKey = ...)` must change to `copy(projectToken = ...)`

## 0.7.2

### Patch Changes

- Use the screen name argument when properties also contain `$screen_name`.

## 0.7.1

### Patch Changes

- Update the PostHog JavaScript SDK to 1.435.8 for JS and Wasm targets.

## 0.7.0

### Minor Changes

- **Breaking:** bump minimum iOS deployment target from 13.0 to 15.0

## 0.6.0

### Minor Changes

- Add native Apple Silicon macOS support through the macosArm64 target.

## 0.5.2

### Patch Changes

- Fix Wasm isFeatureEnabled ignoring a true defaultValue when the JavaScript flag value is undefined.

## 0.5.1

### Patch Changes

- Fix boolean properties being sent as `1`/`0` instead of `true`/`false` on iOS — existing filters need the property type set to Boolean under Data management, and anonymous events now correctly skip person profiles when `personProfiles` is `identifiedOnly`.

## 0.5.0

### Minor Changes

- Use Kotlin's official SwiftPM integration on iOS and include PostHog privacy manifests in Xcode applications.

## 0.4.2

### Patch Changes

- Drop events when a before-send callback throws.

## 0.4.1

### Patch Changes

- Fix iOS linking when consumers use a different Xcode installation path.

## 0.4.0

### Minor Changes

- Add error tracking configuration with automatic exception capture.

## 0.3.0

### Minor Changes

- Add before-send callbacks for modifying or dropping events on Android, iOS, JVM, JS, and Wasm.

## 0.2.2

### Patch Changes

- Update the Android SDK to 3.56.4 and the JVM core SDK to 6.27.0, bringing error tracking, session replay, and performance fixes.

## 0.2.1

### Patch Changes

- Fix Compose Desktop release networking with a compatible Okio version and document the required packaging configuration.

## 0.2.0

### Minor Changes

- Add Kotlin/Wasm browser support backed by posthog-js.

## 0.1.0 — 2026-07-16

### Minor changes

- [8bac955](https://github.com/posthog/posthog-kmp/commit/8bac9551fc7aa9c519ddd6159e29034fe9a7346c) Add a JVM (desktop) target backed by `com.posthog:posthog`, the pure-JVM core library the Android SDK is built on. Event capture, identification, feature flags, groups, error tracking and super properties are fully supported; the offline event queue and identity cache persist under `~/.posthog-kmp`. Mobile/browser-only options (session recording, lifecycle events, screen-view autocapture, deep links) are ignored on the JVM. — Thanks @turnipdabeets!

## 0.0.2 — 2026-07-14

### Patch changes

- [f23ae08](https://github.com/posthog/posthog-kmp/commit/f23ae082b7d64d7e81d96b53e67097f16fd3fda9) Migrate the Android target to the `com.android.kotlin.multiplatform.library` plugin (replacing `com.android.library`), ahead of AGP 9.0 dropping support for the old combination. The published Android artifact now has a single variant instead of a release build type variant. — Thanks @turnipdabeets!

## 0.0.1 — 2026-07-14

### Patch changes

- [cf0125c](https://github.com/posthog/posthog-kmp/commit/cf0125c2a85ec3d958873c08dd3c3c67ed1d35d9) Initial public release of the PostHog Kotlin Multiplatform SDK: event capture, user identification, feature flags, group analytics, screen tracking, error tracking, session recording (Android/iOS), and session management for Android, iOS, and Web (JS). — Thanks @turnipdabeets!

