---
title: "PostHog Kotlin Multiplatform SDK"
sidebar: Docs
showTitle: true
contentMaxWidthClass: max-w-full
platformLogo: kmp
tableOfContents:
  - value: "Getting started"
    url: getting-started
    depth: 0
  - value: "Setup"
    url: setup
    depth: 0
  - value: "Capture"
    url: capture
    depth: 0
  - value: "Identify"
    url: identify
    depth: 0
  - value: "Alias"
    url: alias
    depth: 0
  - value: "Capture exception"
    url: capture-exception
    depth: 0
  - value: "Debug"
    url: debug
    depth: 0
  - value: "Flush"
    url: flush
    depth: 0
  - value: "Get anonymous ID"
    url: get-anonymous-id
    depth: 0
  - value: "Get distinct ID"
    url: get-distinct-id
    depth: 0
  - value: "Get feature flag"
    url: get-feature-flag
    depth: 0
  - value: "Get feature flag result"
    url: get-feature-flag-result
    depth: 0
  - value: "Get feature flags"
    url: get-feature-flags
    depth: 0
  - value: "Get session ID"
    url: get-session-id
    depth: 0
  - value: "Group"
    url: group
    depth: 0
  - value: "Is feature enabled"
    url: is-feature-enabled
    depth: 0
  - value: "Is opt out"
    url: is-opt-out
    depth: 0
  - value: "Opt in"
    url: opt-in
    depth: 0
  - value: "Register"
    url: register
    depth: 0
  - value: "Reload feature flags"
    url: reload-feature-flags
    depth: 0
  - value: "Reset"
    url: reset
    depth: 0
  - value: "Screen"
    url: screen
    depth: 0
  - value: "Set person properties"
    url: set-person-properties
    depth: 0
  - value: "Shutdown"
    url: shutdown
    depth: 0
  - value: "Unregister"
    url: unregister
    depth: 0
  - value: "SDK utils"
    url: sdk-utils
    depth: 0
  - value: "Types"
    url: types
    depth: 0
---

## Getting started


The Kotlin Multiplatform SDK runs in shared Kotlin code on Android, iOS, macOS, the web (JS and Wasm) and the JVM. It delegates to the native PostHog SDK on each target.

To send your first event, add the dependency, then call `PostHog.setup` once at app startup with your project token and host from your [project settings](https://app.posthog.com/project/settings).

Add the SDK to your shared module's `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.posthog:posthog-kmp:<version>")
        }
    }
}
```

On iOS and macOS, the SDK links `posthog-ios` with Kotlin's SwiftPM integration. Run the linkage task once and commit what it generates:

```bash
XCODEPROJ_PATH="$PWD/iosApp/iosApp.xcodeproj" ./gradlew :shared:integrateLinkagePackage
```

Initialize it once. On Android, call it from `Application.onCreate` and pass the `Application`. On every other platform, use `PostHogContext()`:

```kotlin
PostHog.setup(
    config = PostHogConfig(projectToken = "<ph_project_token>", host = "<ph_client_api_host>"),
    context = PostHogContext(this), // PostHogContext() outside Android
)
```

Capture an event and identify the user:

```kotlin
PostHog.capture("button_clicked", properties = mapOf("plan" to "pro"))
PostHog.identify("user-123", userProperties = mapOf("email" to "max@example.com"))
```

Flush queued events before the app process ends:

```kotlin
PostHog.flush()
PostHog.close()
```

## Setup

### `PostHog.setup`

```kotlin
fun setup(config: PostHogConfig, context: PostHogContext)
```

Initialize the PostHog SDK with the given configuration and platform context.

This is the recommended way to initialize PostHog as it avoids storing the context statically, preventing potential memory leaks.

This should be called once at application startup before any other PostHog methods.

#### Parameters

- `config`: PostHog configuration with your API key and options
- `context`: Platform-specific context (Application on Android, empty on other platforms)

## Capture

### `PostHog.capture`

```kotlin
fun capture(event: String, properties: Map<String, Any?>? = null, options: CaptureOptions? = null)
```

Capture an event with optional properties.

Properties with null values are dropped so all platforms send the same event shape. Groups from CaptureOptions.groups are merged with groups registered via group (they do not replace them).

#### Parameters

- `event`: The event name (e.g., "button\_clicked", "purchase\_completed")
- `properties`: Optional properties to include with the event
- `options`: Optional capture options (groups, timestamp)

## Identify

### `PostHog.identify`

```kotlin
fun identify(distinctId: String, userProperties: Map<String, Any?>? = null, userPropertiesSetOnce: Map<String, Any?>? = null)
```

Identify a user with their unique ID and optional properties.

Call this when a user logs in or when you know their identity.

#### Parameters

- `distinctId`: The unique identifier for this user
- `userProperties`: Optional properties to set on the user profile
- `userPropertiesSetOnce`: Optional properties to set only if not already set

## Alias

### `PostHog.alias`

```kotlin
fun alias(alias: String)
```

Create an alias for the current user's distinct ID.

This is useful when you want to associate an anonymous user with an identified user ID.

#### Parameters

- `alias`: The new alias to assign to the current user

## Capture exception

### `PostHog.captureException`

```kotlin
fun captureException(throwable: Throwable, additionalProperties: Map<String, Any?>? = null)
```

Capture an exception for error tracking.

#### Parameters

- `throwable`: The exception to capture
- `additionalProperties`: Optional additional context

## Debug

### `PostHog.setDebug`

```kotlin
fun setDebug(enabled: Boolean)
```

Enable or disable debug logging.

#### Parameters

- `enabled`: true to enable debug logs

## Flush

### `PostHog.flush`

```kotlin
fun flush()
```

Flush all queued events immediately.

Events are normally batched and sent periodically. Call this to force immediate delivery, for example before app close.

## Get anonymous ID

### `PostHog.getAnonymousId`

```kotlin
fun getAnonymousId(): String?
```

Get the current anonymous ID.

The anonymous ID is a randomly generated identifier that persists until the user is identified or the session is reset. On Web this is the stored device id (`$device_id`), which posthog-js does not expose directly.

#### Return

The current anonymous ID or null if not available

## Get distinct ID

### `PostHog.getDistinctId`

```kotlin
fun getDistinctId(): String?
```

Get the current user's distinct ID.

#### Return

The current distinct ID or null if not available

## Get feature flag

### `PostHog.getFeatureFlag`

```kotlin
fun getFeatureFlag(key: String, sendFeatureFlagEvent: Boolean? = null): Any?
```

Get the value of a feature flag.

Feature flags can return boolean, string variants, or JSON payloads.

#### Return

The flag value or null if not found

#### Parameters

- `key`: The feature flag key
- `sendFeatureFlagEvent`: Whether to send a `$feature_flag_called` event for this lookup; null (the default) falls back to PostHogConfig.sendFeatureFlagEvent

## Get feature flag result

### `PostHog.getFeatureFlagResult`

```kotlin
fun getFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean? = null): FeatureFlagResult?
```

Get detailed information about a feature flag evaluation.

This method provides more information than getFeatureFlag, including the reason for the evaluation result and any associated payload.

#### Return

A FeatureFlagResult with detailed evaluation information

#### Parameters

- `key`: The feature flag key
- `sendFeatureFlagEvent`: Whether to send a `$feature_flag_called` event for this lookup; null (the default) falls back to PostHogConfig.sendFeatureFlagEvent

## Get feature flags

### `PostHog.getAllFeatureFlags`

```kotlin
fun getAllFeatureFlags(): Map<String, FeatureFlagResult>
```

Get all feature flags for the current user.

This is a synchronous read of the locally cached flags and does not send a `$feature_flag_called` event. Values are FeatureFlagResult instances keyed by flag key.

#### Return

Map of flag keys to their FeatureFlagResult

## Get session ID

### `PostHog.getSessionId`

```kotlin
fun getSessionId(): String?
```

Get the current session ID.

Session IDs are used to group events that occur within a single user session.

#### Return

The current session ID or null if not available

## Group

### `PostHog.group`

```kotlin
fun group(type: String, key: String, groupProperties: Map<String, Any?>? = null)
```

Associate the current user with a group.

Group analytics allows you to analyze behavior at the organization/team level.

#### Parameters

- `type`: The group type (e.g., "company", "team")
- `key`: The unique identifier for this group
- `groupProperties`: Optional properties to set on the group

## Is feature enabled

### `PostHog.isFeatureEnabled`

```kotlin
fun isFeatureEnabled(key: String, defaultValue: Boolean = false, sendFeatureFlagEvent: Boolean? = null): Boolean
```

Check if a feature flag is enabled.

#### Return

true if the flag is enabled, defaultValue otherwise

#### Parameters

- `key`: The feature flag key
- `defaultValue`: Value to return if the flag is not found (defaults to false)
- `sendFeatureFlagEvent`: Whether to send a `$feature_flag_called` event for this lookup; null (the default) falls back to PostHogConfig.sendFeatureFlagEvent

## Is opt out

### `PostHog.isOptedOut`

```kotlin
fun isOptedOut(): Boolean
```

Check if the user has opted out.

#### Return

true if opted out, false otherwise

## Opt in

### `PostHog.optIn`

```kotlin
fun optIn()
```

Opt back into analytics tracking.

### `PostHog.optOut`

```kotlin
fun optOut()
```

Opt out of analytics tracking.

When opted out, no events will be captured or sent.

## Register

### `PostHog.register`

```kotlin
fun register(key: String, value: Any?)
```

Register a super property that will be sent with every event.

A null value is ignored (use unregister to remove a super property).

#### Parameters

- `key`: The property key
- `value`: The property value

## Reload feature flags

### `PostHog.reloadFeatureFlags`

```kotlin
fun reloadFeatureFlags(callback: (() -> Unit)? = null)
```

Reload feature flags from the server.

Call this to get fresh feature flag values, for example after updating user properties that affect flag targeting.

#### Parameters

- `callback`: Optional callback when flags are loaded

## Reset

### `PostHog.reset`

```kotlin
fun reset()
```

Reset the current user's identity.

Call this when a user logs out to clear their identity and start tracking as an anonymous user again.

## Screen

### `PostHog.screen`

```kotlin
fun screen(screenName: String, properties: Map<String, Any?>? = null)
```

Capture a screen/page view event.

Properties with null values are dropped so all platforms send the same event shape.

#### Parameters

- `screenName`: The name of the screen or page. It takes precedence over a `$screen_name` key in properties.
- `properties`: Optional additional properties

## Set person properties

### `PostHog.setPersonProperties`

```kotlin
fun setPersonProperties(userPropertiesToSet: Map<String, Any>?, userPropertiesToSetOnce: Map<String, Any>? = null)
```

Set properties on the current person profile.

#### Parameters

- `userPropertiesToSet`: Properties to set on the person profile (will overwrite existing values)
- `userPropertiesToSetOnce`: Properties to set only if they do not already exist on the profile

## Shutdown

### `PostHog.close`

```kotlin
fun close()
```

Close the PostHog instance and release resources.

Call this when your application is shutting down.

## Unregister

### `PostHog.unregister`

```kotlin
fun unregister(key: String)
```

Unregister a super property.

#### Parameters

- `key`: The property key to remove

## SDK utils

### `PostHogContext`

```kotlin
fun PostHogContext(): PostHogContext
```

Creates a default PostHogContext for non-Android platforms. On Android, you must use PostHogContext(application) instead.

## Types

### `CaptureOptions`

```kotlin
data class CaptureOptions(val groups: Map<String, String>? = null, val timestamp: Long? = null)
```

Options for capturing events.

#### Properties

- `val groups: Map<String, String>?`: Groups to associate with this event
- `val timestamp: Long?`: Custom event time as Unix epoch milliseconds, representing a UTC instant (defaults to now). UTC is preferred when deriving this value from a date and time; resolve any non-UTC offset to the equivalent instant before converting it to epoch milliseconds. Platform bridges preserve this instant when constructing their native date type.

### `ErrorTrackingConfig`

```kotlin
data class ErrorTrackingConfig(val autoCapture: Boolean = false, val inAppIncludes: List<String> = emptyList(), val ignoredExceptionTypes: List<KClass<out Throwable>> = emptyList(), val inAppExcludes: List<String> = emptyList(), val inAppByDefault: Boolean = true)
```

Error tracking configuration options.

#### Properties

- `val autoCapture: Boolean`: Automatically capture unhandled exceptions.
- `val ignoredExceptionTypes: List<KClass<out Throwable>>`: Supported on Android, JVM, and iOS.
- `val inAppByDefault: Boolean`: iOS and macOS only.
- `val inAppExcludes: List<String>`: iOS and macOS only.
- `val inAppIncludes: List<String>`: Supported on Android, JVM, and iOS.

### `FeatureFlagResult`

```kotlin
data class FeatureFlagResult(val key: String, val enabled: Boolean, val variant: String? = null, val payload: Any? = null)
```

Result of a feature flag evaluation with detailed information.

#### Properties

- `val enabled: Boolean`: whether the feature flag is enabled
- `val key: String`: the key of the feature flag
- `val payload: Any?`: Optional JSON payload associated with the flag
- `val value: Any`: Returns the effective value of the feature flag. For multivariate flags, returns the variant string. For boolean flags, returns the enabled boolean.
- `val variant: String?`: the variant of the feature flag (null for boolean flags)

#### Functions

- `inline fun <reified T> getPayloadAs(): T?`: Returns the payload cast as the specified type.

### `PersonProfiles`

```kotlin
enum class PersonProfiles : Enum<PersonProfiles>
```

Person profile modes for feature flag targeting and person data.

#### Entries

- `ALWAYS`: Create person profiles for all users
- `IDENTIFIED_ONLY`: Only create profiles for identified users
- `NEVER`: Never create person profiles

### `PostHogBeforeSend`

```kotlin
fun interface PostHogBeforeSend
```

Synchronous hook invoked before an event is queued.

Callbacks run in configuration order. Return the same or a modified PostHogEvent to continue, or `null` to drop the event and skip the remaining callbacks. If a callback throws, the event is dropped and the remaining callbacks are skipped.

#### Functions

- `fun run(event: PostHogEvent): PostHogEvent?`

### `PostHogConfig`

```kotlin
data class PostHogConfig(val projectToken: String, val host: String = "https://us.i.posthog.com", val debug: Boolean = false, val captureApplicationLifecycleEvents: Boolean = true, val captureScreenViews: Boolean = false, val captureDeepLinks: Boolean = true, val sendFeatureFlagEvent: Boolean = true, val preloadFeatureFlags: Boolean = true, val flushAt: Int = 20, val flushIntervalSeconds: Int = 30, val maxQueueSize: Int = 1000, val maxBatchSize: Int = 50, val optOut: Boolean = false, val personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY, val sessionRecording: SessionRecordingConfig? = null, val autocapture: Boolean = false, val beforeSend: List<PostHogBeforeSend> = emptyList(), val errorTracking: ErrorTrackingConfig? = null, val capturePushNotificationSubscriptions: Boolean = true, val capturePushNotificationOpened: Boolean = true)
```

Configuration options for PostHog SDK initialization.

#### Properties

- `val apiKey: String`: Deprecated alias for projectToken.
- `val autocapture: Boolean`: Enable automatic event capture (platform dependent)
- `val beforeSend: List<PostHogBeforeSend>`: Synchronous callbacks that can modify or drop events before they are queued
- `val captureApplicationLifecycleEvents: Boolean`: Automatically capture app lifecycle events
- `val captureDeepLinks: Boolean`: Capture deep link opens (Android only)
- `val capturePushNotificationOpened: Boolean`: Capture push notification opens automatically (Android only, defaults to true).
- `val capturePushNotificationSubscriptions: Boolean`: Auto-register the FCM token at startup when Firebase Messaging is available (Android only, defaults to true).
- `val captureScreenViews: Boolean`: Automatically capture screen/page views
- `val debug: Boolean`: Enable debug logging
- `val errorTracking: ErrorTrackingConfig?`: Error tracking configuration (platform dependent)
- `val flushAt: Int`: Number of events to batch before sending
- `val flushIntervalSeconds: Int`: Interval in seconds between automatic flushes
- `val host: String`: PostHog instance URL (defaults to US cloud)
- `val maxBatchSize: Int`: Maximum events per batch
- `val maxQueueSize: Int`: Maximum number of events to queue
- `val optOut: Boolean`: Start with analytics opted out
- `val personProfiles: PersonProfiles`: Person profile mode for feature flag targeting
- `val preloadFeatureFlags: Boolean`: Preload feature flags on initialization
- `val projectToken: String`: Your PostHog project token (required), which starts with `phc_`. Find it in your project settings: https://us.posthog.com/settings/project-details#variables. Formerly named `apiKey`. Example: `PostHogConfig(projectToken = "phc_your_project_token")`.
- `val sendFeatureFlagEvent: Boolean`: Send events when feature flags are evaluated
- `val sessionRecording: SessionRecordingConfig?`: Enable session recording (platform dependent)

#### Companion

- `const val HOST_EU: String`: PostHog EU Cloud instance
- `const val HOST_US: String`: PostHog US Cloud instance
- `operator fun invoke(apiKey: String, host: String = "https://us.i.posthog.com", debug: Boolean = false, captureApplicationLifecycleEvents: Boolean = true, captureScreenViews: Boolean = false, captureDeepLinks: Boolean = true, sendFeatureFlagEvent: Boolean = true, preloadFeatureFlags: Boolean = true, flushAt: Int = 20, flushIntervalSeconds: Int = 30, maxQueueSize: Int = 1000, maxBatchSize: Int = 50, optOut: Boolean = false, personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY, sessionRecording: SessionRecordingConfig? = null, autocapture: Boolean = false, beforeSend: List<PostHogBeforeSend> = emptyList(), errorTracking: ErrorTrackingConfig? = null): PostHogConfig`: Deprecated named-argument form of the PostHogConfig constructor that takes `apiKey`.

### `PostHogContext`

```kotlin
class PostHogContext
```

Platform-specific context required for PostHog initialization.

On Android, this wraps the Application context. On other platforms, this is an empty marker object.

#### Usage

```kotlin
// Android
val context = PostHogContext(application)

// iOS/Web/JVM/macOS
val context = PostHogContext()

// Setup PostHog with context
PostHog.setup(config, context)
```

On android:

```kotlin
class PostHogContext(val application: Application)
```

Android-specific PostHog context that wraps the Application.

#### Properties

- `val application: Application`: The Android Application instance

### `PostHogEvent`

```kotlin
data class PostHogEvent(val event: String, val distinctId: String, val properties: Map<String, Any?>)
```

Event passed to PostHogBeforeSend callbacks before it is queued.

Return a copied event to change its name, distinct ID, or properties without mutating the original event. Platform metadata not exposed here, such as the timestamp and UUID, is preserved.

#### Properties

- `val distinctId: String`
- `val event: String`
- `val properties: Map<String, Any?>`

### `SessionRecordingConfig`

```kotlin
data class SessionRecordingConfig(val enabled: Boolean = true, val maskAllTextInputs: Boolean = true, val maskAllImages: Boolean = true, val captureNetworkTelemetry: Boolean = true, val captureLogs: Boolean = false, val screenshot: Boolean = false, val captureLogcat: Boolean = true, val debouncerDelayMs: Long = 1000, val verifyScreenshotMaskAlignment: Boolean = false)
```

Session recording configuration options.

Defaults match the native Android and iOS SDK defaults so wrapping adds no drift.

#### Properties

- `val captureLogcat: Boolean`: Capture Android logcat output (Android only)
- `val captureLogs: Boolean`: Capture console logs (iOS/Web)
- `val captureNetworkTelemetry: Boolean`: Include network requests in recording
- `val debouncerDelayMs: Long`: Delay in milliseconds for debouncing touch events (Android only)
- `val enabled: Boolean`: Enable session recording
- `val maskAllImages: Boolean`: Mask all images
- `val maskAllTextInputs: Boolean`: Mask all text input values
- `val screenshot: Boolean`: Enable screenshot mode instead of wireframe (Android/iOS experimental). When enabled, the SDK takes actual screenshots instead of wireframe representations. WARNING: Screenshots may contain sensitive information - ensure proper masking.
- `val verifyScreenshotMaskAlignment: Boolean`: Experimental screenshot mask-alignment verification (Android only). Can preserve captures during pixel-only redraws, but adds view-hierarchy walks during capture. Defaults to false and is ignored on other platforms. Windows with a Compose root always use verification regardless of this flag. Only applies when screenshot is enabled.
