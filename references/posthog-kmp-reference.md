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
  - value: "Capture AI"
    url: capture-ai
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
  - value: "On feature flags"
    url: on-feature-flags
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
  - value: "Reset group properties for flags"
    url: reset-group-properties-for-flags
    depth: 0
  - value: "Reset person properties for flags"
    url: reset-person-properties-for-flags
    depth: 0
  - value: "Screen"
    url: screen
    depth: 0
  - value: "Set group properties for flags"
    url: set-group-properties-for-flags
    depth: 0
  - value: "Set person properties"
    url: set-person-properties
    depth: 0
  - value: "Set person properties for flags"
    url: set-person-properties-for-flags
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
- `options`: Optional capture options (groups, timestamp, event options)

## Identify

### `PostHog.identify`

```kotlin
fun identify(distinctId: String, userProperties: Map<String, Any?>? = null, userPropertiesSetOnce: Map<String, Any?>? = null, eventOptions: Map<String, Any?>? = null)
```

Identify a user with their unique ID and optional properties.

Call this when a user logs in or when you know their identity.

#### Parameters

- `distinctId`: The unique identifier for this user
- `userProperties`: Optional properties to set on the user profile
- `userPropertiesSetOnce`: Optional properties to set only if not already set
- `eventOptions`: Settings that tell PostHog how to process the `$identify` or `$set` event this call sends. Dropped on JavaScript and Wasm.

## Alias

### `PostHog.alias`

```kotlin
fun alias(alias: String, eventOptions: Map<String, Any?>? = null)
```

Create an alias for the current user's distinct ID.

This is useful when you want to associate an anonymous user with an identified user ID.

#### Parameters

- `alias`: The new alias to assign to the current user
- `eventOptions`: Settings that tell PostHog how to process the `$create_alias` event. Dropped on JavaScript and Wasm.

## Capture AI

### `PostHog.captureAi`

```kotlin
fun captureAi(event: String, properties: Map<String, Any?>? = null, options: CaptureOptions? = null): String?
```

Capture an AI observability event, such as `$ai_generation`, `$ai_span`, `$ai_trace` or `$ai_embedding`, and send it to PostHog's AI endpoint, which accepts much larger events than capture.

Takes the same arguments as capture and goes through the same PostHogConfig.beforeSend callbacks. The event is queued separately from other events and sent unchanged: the SDK doesn't redact or truncate prompts, outputs or media. PostHog drops an event sent here whose name doesn't start with `$ai_`. capture never sends events here, even ones named `$ai_*`.

Supported on Android, JVM, iOS and macOS. On JavaScript and Wasm it sends nothing and returns `null`.

```kotlin
PostHog.captureAi(
    "\$ai_generation",
    mapOf("\$ai_trace_id" to traceId, "\$ai_model" to "gpt-5", "\$ai_provider" to "openai")
)
```

#### Return

The event's UUID once the SDK has queued it, or `null` when it won't be sent: the SDK is not set up, disabled or opted out, a PostHogConfig.beforeSend callback dropped it, the event is too large, or the platform is JavaScript or Wasm

#### Parameters

- `event`: The AI event name, such as `$ai_generation`
- `properties`: Optional properties to include with the event
- `options`: Optional capture options (groups, timestamp, event options)

## Capture exception

### `PostHog.captureException`

```kotlin
fun captureException(throwable: Throwable, additionalProperties: Map<String, Any?>? = null, eventOptions: Map<String, Any?>? = null)
```

Capture an exception for error tracking.

#### Parameters

- `throwable`: The exception to capture
- `additionalProperties`: Optional additional context
- `eventOptions`: Settings that tell PostHog how to process the `$exception` event

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

Returns `true` or `false` for a boolean flag, or the variant key as a `String` for a multivariate flag. Use getFeatureFlagResult to read a flag's payload.

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
fun group(type: String, key: String, groupProperties: Map<String, Any?>? = null, eventOptions: Map<String, Any?>? = null)
```

Associate the current user with a group.

Group analytics allows you to analyze behavior at the organization/team level.

#### Parameters

- `type`: The group type (e.g., "company", "team")
- `key`: The unique identifier for this group
- `groupProperties`: Optional properties to set on the group
- `eventOptions`: Settings that tell PostHog how to process the `$groupidentify` event. Dropped on JavaScript and Wasm.

## Is feature enabled

### `PostHog.isFeatureEnabled`

```kotlin
fun isFeatureEnabled(key: String, defaultValue: Boolean = false, sendFeatureFlagEvent: Boolean? = null): Boolean
```

Check if a feature flag is enabled.

#### Return

true if the flag is enabled, false if it is disabled, or defaultValue if it is not found

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

## On feature flags

### `PostHog.onFeatureFlags`

```kotlin
fun onFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription
```

Register a callback that's invoked whenever feature flags are loaded or updated.

It's invoked after flags load at startup, after reloadFeatureFlags, and after calls that reload flags such as identify, group or reset. If flags have already loaded when you register, it's also invoked with the current values shortly afterwards. When a load fails, PostHogFeatureFlagsLoaded.errorsLoading is `true` and the values are the last known flags. Treat each invocation as "flags may have changed", not as an exactly-once event.

You can register before setup. On Android, JVM, iOS and macOS the registration stays active across close and setup until you unsubscribe; on JavaScript and Wasm, close shuts posthog-js down for the page. On Android, iOS and macOS the callback runs on the main thread.

```kotlin
val subscription = PostHog.onFeatureFlags { loaded ->
    showNewCheckout = loaded.variants["new-checkout"] == "test"
}
// Later, stop listening
subscription.unsubscribe()
```

#### Return

A PostHogFeatureFlagsSubscription; call PostHogFeatureFlagsSubscription.unsubscribe to stop

#### Parameters

- `callback`: Receives the enabled flags, their values, and whether loading failed

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

### `PostHog.registerOptions`

```kotlin
fun registerOptions(options: Map<String, Any?>)
```

Register event options sent with every later event, such as PostHogEventOptions.COOKIELESS\_MODE. Keys are sent as given, including ones PostHogEventOptions doesn't list.

An event's own options win over registered ones key by key. Keys set here overwrite registered values for the same key, and `null` values are ignored. Registered options persist across app restarts until unregisterOptions removes them or reset clears them. On Android, JVM, iOS and macOS, PostHogConfig.beforeSend callbacks see them in PostHogEvent.options.

On Android, JVM, iOS and macOS, a registered PostHogEventOptions.PROCESS\_PERSON\_PROFILE skips the events identify, alias, group and setPersonProperties send. A value passed on those calls still applies.

Use this instead of registering a legacy property such as `$cookieless_mode`.

On JavaScript and Wasm, posthog-js has no options, so the keys PostHogEventOptions lists are registered as their legacy properties, with the limits described there, and other keys are dropped. PostHogConfig.beforeSend callbacks see them in PostHogEvent.properties there.

```kotlin
PostHog.registerOptions(mapOf(PostHogEventOptions.COOKIELESS_MODE to true))
```

#### Parameters

- `options`: The options to register, keyed by option key

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
fun reloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)? = null)
```

Reload feature flags from the server.

Call this to get fresh feature flag values, for example after updating user properties that affect flag targeting.

The callback runs once, when the reload finishes, whether or not it succeeded. If the request fails, PostHogFeatureFlagsLoaded.errorsLoading is `true` and the values are the last known flags. Before setup, it runs right away with no flags and `errorsLoading` set to `true`. On JavaScript and Wasm, posthog-js notifies no listener when the flags quota is exhausted or a failed request has no cached flags, so the callback waits for the next load that does. It can run on a background thread on Android, JVM, iOS and macOS.

```kotlin
PostHog.reloadFeatureFlags { loaded ->
    if (!loaded.errorsLoading && "new-checkout" in loaded.flags) {
        // Show the new checkout
    }
}
```

#### Parameters

- `callback`: Optional callback with the loaded flags; null (the default) reloads without one

## Reset

### `PostHog.reset`

```kotlin
fun reset()
```

Reset the current user's identity.

Call this when a user logs out to clear their identity and start tracking as an anonymous user again.

## Reset group properties for flags

### `PostHog.resetGroupPropertiesForFlags`

```kotlin
fun resetGroupPropertiesForFlags(type: String? = null, reloadFeatureFlags: Boolean = true)
```

Clear the group properties set with setGroupPropertiesForFlags.

Feature flags are then evaluated with the group properties PostHog has stored. Before setup, it does nothing.

```kotlin
PostHog.resetGroupPropertiesForFlags("company") // one group type
PostHog.resetGroupPropertiesForFlags() // every group type
```

#### Parameters

- `type`: The group type to clear; null (the default) clears every group type
- `reloadFeatureFlags`: Whether to reload feature flags afterwards (defaults to true)

## Reset person properties for flags

### `PostHog.resetPersonPropertiesForFlags`

```kotlin
fun resetPersonPropertiesForFlags(reloadFeatureFlags: Boolean = true)
```

Clear the person properties set with setPersonPropertiesForFlags.

Feature flags are then evaluated with the person properties PostHog has stored. Before setup, it does nothing.

```kotlin
PostHog.resetPersonPropertiesForFlags()
```

#### Parameters

- `reloadFeatureFlags`: Whether to reload feature flags afterwards (defaults to true)

## Screen

### `PostHog.screen`

```kotlin
fun screen(screenName: String, properties: Map<String, Any?>? = null, eventOptions: Map<String, Any?>? = null)
```

Capture a screen/page view event.

Properties with null values are dropped so all platforms send the same event shape.

#### Parameters

- `screenName`: The name of the screen or page. It takes precedence over a `$screen_name` key in properties.
- `properties`: Optional additional properties
- `eventOptions`: Settings that tell PostHog how to process the `$screen` event

## Set group properties for flags

### `PostHog.setGroupPropertiesForFlags`

```kotlin
fun setGroupPropertiesForFlags(type: String, properties: Map<String, Any?>, reloadFeatureFlags: Boolean = true)
```

Set group properties to use when evaluating feature flags, before they reach the group profile.

Use this when a flag targets a group property that you just set with group, so the flag doesn't wait for PostHog to process it. Calls merge per group type: a later value for the same key replaces the earlier one. The properties are stored on the device until you clear them with resetGroupPropertiesForFlags. Null values are dropped, and an empty map does nothing. No event is sent. Before setup, it does nothing.

```kotlin
PostHog.setGroupPropertiesForFlags("company", mapOf("plan" to "enterprise"))
```

#### Parameters

- `type`: The group type (e.g., "company", "team")
- `properties`: Group properties to send with feature flag requests
- `reloadFeatureFlags`: Whether to reload feature flags afterwards (defaults to true)

## Set person properties

### `PostHog.setPersonProperties`

```kotlin
fun setPersonProperties(userPropertiesToSet: Map<String, Any>?, userPropertiesToSetOnce: Map<String, Any>? = null, eventOptions: Map<String, Any?>? = null)
```

Set properties on the current person profile.

#### Parameters

- `userPropertiesToSet`: Properties to set on the person profile (will overwrite existing values)
- `userPropertiesToSetOnce`: Properties to set only if they do not already exist on the profile
- `eventOptions`: Settings that tell PostHog how to process the `$set` event. Dropped on JavaScript and Wasm.

## Set person properties for flags

### `PostHog.setPersonPropertiesForFlags`

```kotlin
fun setPersonPropertiesForFlags(properties: Map<String, Any?>, reloadFeatureFlags: Boolean = true)
```

Set person properties to use when evaluating feature flags, before they reach the person profile.

Use this when a flag targets a person property that you just set with identify or setPersonProperties, so the flag doesn't wait for PostHog to process it. Calls merge: a later value for the same key replaces the earlier one. The properties are stored on the device until you clear them with resetPersonPropertiesForFlags. Null values are dropped, and an empty map does nothing. No event is sent. Before setup, it does nothing.

```kotlin
PostHog.setPersonPropertiesForFlags(mapOf("plan" to "pro"))
```

#### Parameters

- `properties`: Person properties to send with feature flag requests
- `reloadFeatureFlags`: Whether to reload feature flags afterwards (defaults to true)

## Shutdown

### `PostHog.close`

```kotlin
fun close()
```

Close the PostHog instance and release resources.

Call this when your application is shutting down.

## Unregister

### `PostHog.unregisterOptions`

```kotlin
fun unregisterOptions(key: String)
```

Remove an option registered with registerOptions. Events captured afterwards no longer get it, unless they set it in their own options.

```kotlin
PostHog.unregisterOptions(PostHogEventOptions.COOKIELESS_MODE)
```

#### Parameters

- `key`: The option key, such as PostHogEventOptions.COOKIELESS\_MODE

### `PostHog.unregister`

```kotlin
fun unregister(key: String)
```

Unregister a super property.

#### Parameters

- `key`: The property key to remove

## SDK utils

### `PostHog.setAppLanguage`

```kotlin
fun setAppLanguage(language: String?)
```

Sets the `$app_language` property: the language your app displays, sent with every event.

Default: Android 13+ detects it from the app's `localeConfig`, and iOS and macOS from the localizations the app bundle declares. Android 6-12 and JVM send no `$app_language` until you call this. JavaScript and Wasm don't send it yet, because posthog-js doesn't support it; use register there instead. Compare `$locale`, which is the device language.

The value overrides the detected language. It is trimmed and `_` becomes `-`. It's kept in memory only, so call this at startup and whenever the language changes; it survives reset, and close clears it. A `$app_language` super property set with register takes precedence. You can call it before setup.

```kotlin
PostHog.setAppLanguage("pt-BR")
PostHog.setAppLanguage(null) // back to the detected language
```

#### Parameters

- `language`: a BCP 47 language tag, such as `de` or `pt-BR`. Pass `null` or a blank string to go back to the detected language

### `PostHogContext`

```kotlin
fun PostHogContext(): PostHogContext
```

Creates a default PostHogContext for non-Android platforms. On Android, you must use PostHogContext(application) instead.

## Types

### `CaptureOptions`

```kotlin
data class CaptureOptions(val groups: Map<String, String>? = null, val timestamp: Long? = null, val eventOptions: Map<String, Any?>? = null)
```

Options for capturing events.

#### Properties

- `val eventOptions: Map<String, Any?>?`: Settings that tell PostHog how to process this event. See PostHogEventOptions.
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

### `IosCaptureMethod`

```kotlin
enum class IosCaptureMethod : Enum<IosCaptureMethod>
```

How iOS captures each session replay screenshot.

#### Entries

- `STANDARD`: Draws the window on the main thread, once the screen has settled. The default.
- `GPU`: Experimental. Copies the window's layers and renders the copy on the GPU, in short main-thread steps. Content from another process, such as a web view's page, is not captured, and Metal content, including Compose Multiplatform screens, is recorded as a placeholder. Falls back to STANDARD when a capture can't be made this way.

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

### `PostHogCompression`

```kotlin
enum class PostHogCompression : Enum<PostHogCompression>
```

How the SDK compresses request bodies.

#### Entries

- `GZIP`: Gzip request bodies
- `NONE`: Send request bodies uncompressed

### `PostHogConfig`

```kotlin
data class PostHogConfig(val projectToken: String, val host: String = "https://us.i.posthog.com", val debug: Boolean = false, val captureApplicationLifecycleEvents: Boolean = true, val captureScreenViews: Boolean = false, val captureDeepLinks: Boolean = true, val sendFeatureFlagEvent: Boolean = true, val preloadFeatureFlags: Boolean = true, val flushAt: Int = 20, val flushIntervalSeconds: Int = 30, val maxQueueSize: Int = 1000, val maxBatchSize: Int = 50, val optOut: Boolean = false, val personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY, val sessionRecording: SessionRecordingConfig? = null, val autocapture: Boolean = false, val beforeSend: List<PostHogBeforeSend> = emptyList(), val errorTracking: ErrorTrackingConfig? = null, val capturePushNotificationSubscriptions: Boolean = true, val capturePushNotificationOpened: Boolean = true, val surveys: Boolean = true, val evaluationContexts: List<String>? = null, val captureAiCompression: PostHogCompression? = null, val captureAiMaxQueueSize: Int? = null, val captureAiTimeout: Int? = null, val captureAiMaxEventBytes: Int? = null)
```

Configuration options for PostHog SDK initialization.

#### Properties

- `val apiKey: String`: Deprecated alias for projectToken.
- `val autocapture: Boolean`: Enable automatic event capture (platform dependent)
- `val beforeSend: List<PostHogBeforeSend>`: Synchronous callbacks that can modify or drop events before they are queued
- `val captureAiCompression: PostHogCompression?`: How PostHog.captureAi request bodies are compressed (defaults to null, which uses the native SDK's default, PostHogCompression.GZIP). Other requests aren't affected. Android, JVM, iOS and macOS only.
- `val captureAiMaxEventBytes: Int?`: Maximum size, in bytes, of a serialized PostHog.captureAi event (defaults to null, which uses the native SDK's default, 8 MiB plus 64 KiB). The SDK drops a larger event, and `captureAi` returns `null`. It can only be lowered: a larger value uses the default and logs a warning. A value of 0 or less uses the default. Android, JVM, iOS and macOS only.
- `val captureAiMaxQueueSize: Int?`: Maximum number of PostHog.captureAi events kept in their on-disk queue before the oldest are dropped (defaults to null, which uses the native SDK's default, 100). The queue also keeps at most 50 MiB of AI events. A value of 0 or less uses the default. Android, JVM, iOS and macOS only.
- `val captureAiTimeout: Int?`: How long, in seconds, a PostHog.captureAi request can go without network activity before it times out (defaults to null, which uses the native SDK's default, 60). A value of 0 or less uses the default. Android, JVM, iOS and macOS only.
- `val captureApplicationLifecycleEvents: Boolean`: Automatically capture app lifecycle events
- `val captureDeepLinks: Boolean`: Capture deep link opens (Android only)
- `val capturePushNotificationOpened: Boolean`: Capture push notification opens automatically (Android only, defaults to true).
- `val capturePushNotificationSubscriptions: Boolean`: Auto-register the FCM token at startup when Firebase Messaging is available (Android only, defaults to true).
- `val captureScreenViews: Boolean`: Automatically capture screen/page views
- `val debug: Boolean`: Enable debug logging
- `val errorTracking: ErrorTrackingConfig?`: Error tracking configuration (platform dependent)
- `val evaluationContexts: List<String>?`: Evaluation context tags that limit which feature flags are evaluated (defaults to null, which evaluates every flag). When set, PostHog evaluates only the flags that have at least one of these evaluation tags, plus flags with no evaluation tags. Example: `PostHogConfig(projectToken = "phc_...", evaluationContexts = listOf("production", "checkout"))`.
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
- `val surveys: Boolean`: Load surveys and show the ones that match (defaults to true). Set to false to keep the SDK from loading or showing surveys. Applies on iOS 15+ and web. On Android, surveys display only when the app also depends on `com.posthog:posthog-android-surveys-compose`. Ignored on macOS and JVM.

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
data class PostHogEvent(val event: String, val distinctId: String, val properties: Map<String, Any?>, val options: Map<String, Any?>? = null)
```

Event passed to PostHogBeforeSend callbacks before it is queued.

Return a copied event to change its name, distinct ID, properties or options without mutating the original event. Platform metadata not exposed here, such as the timestamp and UUID, is preserved.

On iOS and macOS, a number the native SDK stored, such as a value set with PostHog.register or PostHog.registerOptions, arrives as a `Long` even if you passed an `Int`, so compare numbers by value, for example `(value as? Number)?.toLong() == 7L`.

#### Properties

- `val distinctId: String`
- `val event: String`
- `val options: Map<String, Any?>?`: Settings that tell PostHog how to process the event, keyed by option key (see PostHogEventOptions), including the ones the SDK set, such as PostHogEventOptions.PROCESS\_PERSON\_PROFILE `false` for an anonymous user's event with the default PostHogConfig.personProfiles. Removing a key, or setting it to `null`, removes the option. A callback that returns an event with `options = null`, such as one it constructs instead of copying, keeps the options of the event it was given. On JavaScript and Wasm, posthog-js has no options, so this is always `null` there and changing it has no effect.
- `val properties: Map<String, Any?>`

### `PostHogEventOptions`

```kotlin
object PostHogEventOptions
```

Keys for event options: settings that tell PostHog how to process an event, sent apart from its properties. Pass them in a map, such as `CaptureOptions(eventOptions = mapOf(PostHogEventOptions.PROCESS_PERSON_PROFILE to false))`.

Options are sent as given, so a key not listed here, such as one a newer PostHog version reads, also works. A `null` value is left unset, so PostHog and the SDK use their defaults.

These replace the `$process_person_profile`, `$cookieless_mode`, `$ignore_sent_at` and `$product_tour_id` properties. Those properties still work, and an option wins over its property.

On JavaScript and Wasm, posthog-js has no options, so the keys listed here are sent as those properties on PostHog.capture, PostHog.screen and PostHog.captureException events, and dropped by PostHog.identify, PostHog.alias, PostHog.group and PostHog.setPersonProperties, which posthog-js sends without event properties. Other keys are dropped there. posthog-js sets `$process_person_profile` itself, so PROCESS\_PERSON\_PROFILE has no effect there.

#### Properties

- `const val COOKIELESS_MODE: String`: Whether PostHog processes the event in cookieless mode. A `Boolean`.
- `const val DISABLE_SKEW_CORRECTION: String`: Whether PostHog keeps the event timestamp as sent instead of correcting it for device clock skew. A `Boolean`.
- `const val PROCESS_PERSON_PROFILE: String`: Whether PostHog creates or updates a person profile for the event. A `Boolean`.
- `const val PRODUCT_TOUR_ID: String`: The product tour the event belongs to. A `String`.

### `PostHogFeatureFlagsLoaded`

```kotlin
class PostHogFeatureFlagsLoaded
```

The feature flags passed to PostHog.onFeatureFlags and PostHog.reloadFeatureFlags callbacks.

It holds the enabled flags as they were when the callback was invoked, and whether the latest attempt to load them failed. Disabled flags are left out; read them with PostHog.getFeatureFlag.

Example:

```kotlin
PostHog.reloadFeatureFlags { loaded ->
    if (loaded.errorsLoading) {
        // The values are the last known (cached) flags
    }
    if (loaded.variants["new-checkout"] == "test") {
        // Show the test variant
    }
}
```

#### Properties

- `val errorsLoading: Boolean`: `true` if the latest attempt to load feature flags failed.
- `val flags: List<String>`: The keys of the enabled feature flags.
- `val variants: Map<String, Any>`: The values of the enabled feature flags, keyed by flag key.

### `PostHogFeatureFlagsSubscription`

```kotlin
class PostHogFeatureFlagsSubscription
```

A registration returned by PostHog.onFeatureFlags.

The callback stays registered until you call unsubscribe. On Android, JVM, iOS and macOS that includes across PostHog.close and PostHog.setup; on JavaScript and Wasm, PostHog.close shuts posthog-js down for the page. Keep a reference only if you want to unsubscribe later.

Example:

```kotlin
val subscription = PostHog.onFeatureFlags { loaded -> render(loaded.variants) }
// Later, stop listening
subscription.unsubscribe()
```

#### Functions

- `fun unsubscribe()`: Stops the callback from being invoked. Calling it more than once has no effect.

### `SessionRecordingConfig`

```kotlin
data class SessionRecordingConfig(val enabled: Boolean = true, val maskAllTextInputs: Boolean = true, val maskAllImages: Boolean = true, val captureNetworkTelemetry: Boolean = true, val captureLogs: Boolean = false, val captureLogcat: Boolean = true, val throttleDelayMs: Long = 1000, val verifyScreenshotMaskAlignment: Boolean = false, val iosCaptureMethod: IosCaptureMethod = IosCaptureMethod.STANDARD)
```

Session recording configuration options.

Defaults match the native Android and iOS SDK defaults so wrapping adds no drift.

#### Properties

- `val captureLogcat: Boolean`: Capture Android logcat output (Android only)
- `val captureLogs: Boolean`: Capture console logs (iOS/Web)
- `val captureNetworkTelemetry: Boolean`: Include network requests in recording
- `val enabled: Boolean`: Enable session recording
- `val iosCaptureMethod: IosCaptureMethod`: How iOS captures each replay screenshot (iOS only). Defaults to IosCaptureMethod.STANDARD; set the experimental IosCaptureMethod.GPU for less main-thread time on screens without Compose Multiplatform, web views or Metal content. Ignored on other platforms.
- `val maskAllImages: Boolean`: Mask all images
- `val maskAllTextInputs: Boolean`: Mask all text input values
- `val throttleDelayMs: Long`: Minimum delay in milliseconds between replay snapshots (Android and iOS)
- `val verifyScreenshotMaskAlignment: Boolean`: Experimental screenshot mask-alignment verification (Android only). Can preserve captures during pixel-only redraws, but adds view-hierarchy walks during capture. Defaults to false and is ignored on other platforms. Windows with a Compose root always use verification regardless of this flag.
