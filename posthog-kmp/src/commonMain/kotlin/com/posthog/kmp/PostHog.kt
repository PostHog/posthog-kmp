package com.posthog.kmp

import kotlin.concurrent.Volatile

/**
 * PostHog Kotlin Multiplatform SDK
 *
 * A cross-platform analytics SDK for PostHog that works on Android, iOS, JVM, and Web (JS and Wasm).
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Initialize PostHog
 * PostHog.setup(PostHogConfig(projectToken = "phc_your_project_token"), PostHogContext())
 *
 * // Capture events
 * PostHog.capture("button_clicked", mapOf("button_name" to "submit"))
 *
 * // Identify users
 * PostHog.identify("user_123", mapOf("email" to "user@example.com"))
 *
 * // Check feature flags
 * if (PostHog.isFeatureEnabled("new_feature")) {
 *     // Show new feature
 * }
 * ```
 *
 * @see PostHogConfig for configuration options
 */
public object PostHog {
    /**
     * Initialize the PostHog SDK with the given configuration and platform context.
     *
     * This is the recommended way to initialize PostHog as it avoids storing
     * the context statically, preventing potential memory leaks.
     *
     * This should be called once at application startup before any other PostHog methods.
     *
     * @param config PostHog configuration with your API key and options
     * @param context Platform-specific context (Application on Android, empty on other platforms)
     */
    public fun setup(config: PostHogConfig, context: PostHogContext) {
        currentConfig = config
        platformSetup(config, context)
    }

    // ==================== Event Capture ====================

    /**
     * Capture an event with optional properties.
     *
     * Properties with null values are dropped so all platforms send the same event shape.
     * Groups from [CaptureOptions.groups] are merged with groups registered via [group]
     * (they do not replace them).
     *
     * @param event The event name (e.g., "button_clicked", "purchase_completed")
     * @param properties Optional properties to include with the event
     * @param options Optional capture options (groups, timestamp)
     */
    public fun capture(
        event: String,
        properties: Map<String, Any?>? = null,
        options: CaptureOptions? = null
    ) {
        platformCapture(event, properties?.dropNullValues(), options?.groups, options?.timestamp)
    }

    /**
     * Capture a screen/page view event.
     *
     * Properties with null values are dropped so all platforms send the same event shape.
     *
     * @param screenName The name of the screen or page. It takes precedence over a
     *   `$screen_name` key in [properties].
     * @param properties Optional additional properties
     */
    public fun screen(screenName: String, properties: Map<String, Any?>? = null) {
        platformScreen(screenName, properties?.minus("\$screen_name")?.dropNullValues())
    }

    // ==================== User Identification ====================

    /**
     * Identify a user with their unique ID and optional properties.
     *
     * Call this when a user logs in or when you know their identity.
     *
     * @param distinctId The unique identifier for this user
     * @param userProperties Optional properties to set on the user profile
     * @param userPropertiesSetOnce Optional properties to set only if not already set
     */
    public fun identify(
        distinctId: String,
        userProperties: Map<String, Any?>? = null,
        userPropertiesSetOnce: Map<String, Any?>? = null
    ) {
        platformIdentify(distinctId, userProperties?.dropNullValues(), userPropertiesSetOnce?.dropNullValues())
    }

    /**
     * Create an alias for the current user's distinct ID.
     *
     * This is useful when you want to associate an anonymous user
     * with an identified user ID.
     *
     * @param alias The new alias to assign to the current user
     */
    public fun alias(alias: String) {
        platformAlias(alias)
    }

    /**
     * Reset the current user's identity.
     *
     * Call this when a user logs out to clear their identity
     * and start tracking as an anonymous user again.
     */
    public fun reset() {
        platformReset()
    }

    /**
     * Get the current user's distinct ID.
     *
     * @return The current distinct ID or null if not available
     */
    public fun getDistinctId(): String? {
        return platformGetDistinctId()
    }

    // ==================== Super Properties ====================

    /**
     * Register a super property that will be sent with every event.
     *
     * A null value is ignored (use [unregister] to remove a super property).
     *
     * @param key The property key
     * @param value The property value
     */
    public fun register(key: String, value: Any?) {
        value ?: return
        platformRegister(key, value)
    }

    /**
     * Unregister a super property.
     *
     * @param key The property key to remove
     */
    public fun unregister(key: String) {
        platformUnregister(key)
    }

    /**
     * Sets the `$app_language` property: the language your app displays, sent with every event.
     *
     * Default: Android 13+ detects it from the app's `localeConfig`, and iOS and macOS from the
     * localizations the app bundle declares. Android 6-12 and JVM send no `$app_language` until you
     * call this. JavaScript and Wasm don't send it yet, because posthog-js doesn't support it; use
     * [register] there instead. Compare `$locale`, which is the device language.
     *
     * The value overrides the detected language. It is trimmed and `_` becomes `-`. It's kept in
     * memory only, so call this at startup and whenever the language changes; it survives [reset].
     * A `$app_language` super property set with [register] takes precedence.
     *
     * On Android and JVM, call this after [setup]: a call before [setup] is dropped.
     *
     * ```kotlin
     * PostHog.setAppLanguage("pt-BR")
     * PostHog.setAppLanguage(null) // back to the detected language
     * ```
     *
     * @param language a BCP 47 language tag, such as `de` or `pt-BR`. Pass `null` or a blank string
     *   to go back to the detected language
     */
    public fun setAppLanguage(language: String?) {
        // TODO(app-language-before-setup): drop the "after setup" notes in this KDoc and MIGRATION.md once
        // Android and JVM keep a language set before setup().
        platformSetAppLanguage(language?.trim()?.replace('_', '-')?.ifEmpty { null })
    }

    // ==================== Person Properties ====================

    /**
     * Set properties on the current person profile.
     *
     * @param userPropertiesToSet Properties to set on the person profile (will overwrite existing values)
     * @param userPropertiesToSetOnce Properties to set only if they do not already exist on the profile
     */
    public fun setPersonProperties(
        userPropertiesToSet: Map<String, Any>?,
        userPropertiesToSetOnce: Map<String, Any>? = null
    ) {
        platformSetPersonProperties(userPropertiesToSet, userPropertiesToSetOnce)
    }

    // ==================== Group Analytics ====================

    /**
     * Associate the current user with a group.
     *
     * Group analytics allows you to analyze behavior at the organization/team level.
     *
     * @param type The group type (e.g., "company", "team")
     * @param key The unique identifier for this group
     * @param groupProperties Optional properties to set on the group
     */
    public fun group(
        type: String,
        key: String,
        groupProperties: Map<String, Any?>? = null
    ) {
        platformGroup(type, key, groupProperties?.dropNullValues())
    }

    // ==================== Feature Flags ====================

    /**
     * Check if a feature flag is enabled.
     *
     * @param key The feature flag key
     * @param defaultValue Value to return if the flag is not found (defaults to false)
     * @param sendFeatureFlagEvent Whether to send a `$feature_flag_called` event for this
     *   lookup; null (the default) falls back to [PostHogConfig.sendFeatureFlagEvent]
     * @return true if the flag is enabled, false if it is disabled, or [defaultValue] if it is not found
     */
    public fun isFeatureEnabled(key: String, defaultValue: Boolean = false, sendFeatureFlagEvent: Boolean? = null): Boolean {
        return platformIsFeatureEnabled(key, defaultValue, resolveSendFeatureFlagEvent(sendFeatureFlagEvent))
    }

    /**
     * Get the value of a feature flag.
     *
     * Returns `true` or `false` for a boolean flag, or the variant key as a `String` for a
     * multivariate flag. Use [getFeatureFlagResult] to read a flag's payload.
     *
     * @param key The feature flag key
     * @param sendFeatureFlagEvent Whether to send a `$feature_flag_called` event for this
     *   lookup; null (the default) falls back to [PostHogConfig.sendFeatureFlagEvent]
     * @return The flag value or null if not found
     */
    public fun getFeatureFlag(key: String, sendFeatureFlagEvent: Boolean? = null): Any? {
        return platformGetFeatureFlag(key, resolveSendFeatureFlagEvent(sendFeatureFlagEvent))
    }

    /**
     * Get all feature flags for the current user.
     *
     * This is a synchronous read of the locally cached flags and does not send a
     * `$feature_flag_called` event. Values are [FeatureFlagResult] instances keyed by flag key.
     *
     * @return Map of flag keys to their [FeatureFlagResult]
     */
    public fun getAllFeatureFlags(): Map<String, FeatureFlagResult> {
        return platformGetAllFeatureFlags()
    }

    /**
     * Reload feature flags from the server.
     *
     * Call this to get fresh feature flag values, for example after
     * updating user properties that affect flag targeting.
     *
     * The callback runs once, when the reload finishes, whether or not it succeeded. If the request
     * fails, [PostHogFeatureFlagsLoaded.errorsLoading] is `true` and the values are the last known
     * flags. Before [setup], it runs right away with no flags and `errorsLoading` set to `true`.
     * It can run on a background thread on Android, JVM, iOS and macOS.
     *
     * ```kotlin
     * PostHog.reloadFeatureFlags { loaded ->
     *     if (!loaded.errorsLoading && "new-checkout" in loaded.flags) {
     *         // Show the new checkout
     *     }
     * }
     * ```
     *
     * @param callback Optional callback with the loaded flags; null (the default) reloads without one
     */
    public fun reloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)? = null) {
        platformReloadFeatureFlags(callback)
    }

    /**
     * Register a callback that's invoked whenever feature flags are loaded or updated.
     *
     * It's invoked after flags load at startup, after [reloadFeatureFlags], and after calls that
     * reload flags such as [identify], [group] or [reset]. If flags have already loaded when you
     * register, it's also invoked with the current values shortly afterwards. When a load fails,
     * [PostHogFeatureFlagsLoaded.errorsLoading] is `true` and the values are the last known flags.
     * Treat each invocation as "flags may have changed", not as an exactly-once event.
     *
     * You can register before [setup], and the registration stays active across [close] and [setup]
     * until you unsubscribe. On Android, iOS and macOS the callback runs on the main thread.
     *
     * ```kotlin
     * val subscription = PostHog.onFeatureFlags { loaded ->
     *     showNewCheckout = loaded.variants["new-checkout"] == "test"
     * }
     * // Later, stop listening
     * subscription.unsubscribe()
     * ```
     *
     * @param callback Receives the enabled flags, their values, and whether loading failed
     * @return A [PostHogFeatureFlagsSubscription]; call [PostHogFeatureFlagsSubscription.unsubscribe] to stop
     */
    public fun onFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription {
        return platformOnFeatureFlags(callback)
    }

    /**
     * Set person properties to use when evaluating feature flags, before they reach the person profile.
     *
     * Use this when a flag targets a person property that you just set with [identify] or
     * [setPersonProperties], so the flag doesn't wait for PostHog to process it. Calls merge: a later
     * value for the same key replaces the earlier one. The properties are stored on the device until
     * you clear them with [resetPersonPropertiesForFlags]. Null values are dropped, and an empty map does
     * nothing. No event is sent. Before [setup], it does nothing.
     *
     * ```kotlin
     * PostHog.setPersonPropertiesForFlags(mapOf("plan" to "pro"))
     * ```
     *
     * @param properties Person properties to send with feature flag requests
     * @param reloadFeatureFlags Whether to reload feature flags afterwards (defaults to true)
     */
    public fun setPersonPropertiesForFlags(properties: Map<String, Any?>, reloadFeatureFlags: Boolean = true) {
        val nonNull = properties.dropNullValues() ?: return
        platformSetPersonPropertiesForFlags(nonNull, reloadFeatureFlags)
    }

    /**
     * Clear the person properties set with [setPersonPropertiesForFlags].
     *
     * Feature flags are then evaluated with the person properties PostHog has stored. Before [setup],
     * it does nothing.
     *
     * ```kotlin
     * PostHog.resetPersonPropertiesForFlags()
     * ```
     *
     * @param reloadFeatureFlags Whether to reload feature flags afterwards (defaults to true)
     */
    public fun resetPersonPropertiesForFlags(reloadFeatureFlags: Boolean = true) {
        platformResetPersonPropertiesForFlags(reloadFeatureFlags)
    }

    /**
     * Set group properties to use when evaluating feature flags, before they reach the group profile.
     *
     * Use this when a flag targets a group property that you just set with [group], so the flag
     * doesn't wait for PostHog to process it. Calls merge per group type: a later value for the same
     * key replaces the earlier one. The properties are stored on the device until
     * you clear them with [resetGroupPropertiesForFlags]. Null values are dropped, and an empty map does
     * nothing. No event is sent. Before [setup], it does nothing.
     *
     * ```kotlin
     * PostHog.setGroupPropertiesForFlags("company", mapOf("plan" to "enterprise"))
     * ```
     *
     * @param type The group type (e.g., "company", "team")
     * @param properties Group properties to send with feature flag requests
     * @param reloadFeatureFlags Whether to reload feature flags afterwards (defaults to true)
     */
    public fun setGroupPropertiesForFlags(
        type: String,
        properties: Map<String, Any?>,
        reloadFeatureFlags: Boolean = true
    ) {
        val nonNull = properties.dropNullValues() ?: return
        platformSetGroupPropertiesForFlags(type, nonNull, reloadFeatureFlags)
    }

    /**
     * Clear the group properties set with [setGroupPropertiesForFlags].
     *
     * Feature flags are then evaluated with the group properties PostHog has stored. Before [setup],
     * it does nothing.
     *
     * ```kotlin
     * PostHog.resetGroupPropertiesForFlags("company") // one group type
     * PostHog.resetGroupPropertiesForFlags() // every group type
     * ```
     *
     * @param type The group type to clear; null (the default) clears every group type
     * @param reloadFeatureFlags Whether to reload feature flags afterwards (defaults to true)
     */
    public fun resetGroupPropertiesForFlags(type: String? = null, reloadFeatureFlags: Boolean = true) {
        platformResetGroupPropertiesForFlags(type, reloadFeatureFlags)
    }

    /**
     * Get detailed information about a feature flag evaluation.
     *
     * This method provides more information than [getFeatureFlag], including
     * the reason for the evaluation result and any associated payload.
     *
     * @param key The feature flag key
     * @param sendFeatureFlagEvent Whether to send a `$feature_flag_called` event for this
     *   lookup; null (the default) falls back to [PostHogConfig.sendFeatureFlagEvent]
     * @return A [FeatureFlagResult] with detailed evaluation information
     */
    public fun getFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean? = null): FeatureFlagResult? {
        return platformGetFeatureFlagResult(key, resolveSendFeatureFlagEvent(sendFeatureFlagEvent))
    }

    // ==================== Session Management ====================

    /**
     * Get the current anonymous ID.
     *
     * The anonymous ID is a randomly generated identifier that persists
     * until the user is identified or the session is reset. On Web this is the
     * stored device id (`$device_id`), which posthog-js does not expose directly.
     *
     * @return The current anonymous ID or null if not available
     */
    public fun getAnonymousId(): String? {
        return platformGetAnonymousId()
    }

    /**
     * Get the current session ID.
     *
     * Session IDs are used to group events that occur within a single user session.
     *
     * @return The current session ID or null if not available
     */
    public fun getSessionId(): String? {
        return platformGetSessionId()
    }

    // ==================== Error Tracking ====================

    /**
     * Capture an exception for error tracking.
     *
     * @param throwable The exception to capture
     * @param additionalProperties Optional additional context
     */
    public fun captureException(
        throwable: Throwable,
        additionalProperties: Map<String, Any?>? = null
    ) {
        platformCaptureException(throwable, additionalProperties?.dropNullValues())
    }

    // ==================== Opt In/Out ====================

    /**
     * Opt out of analytics tracking.
     *
     * When opted out, no events will be captured or sent.
     */
    public fun optOut() {
        platformOptOut()
    }

    /**
     * Opt back into analytics tracking.
     */
    public fun optIn() {
        platformOptIn()
    }

    /**
     * Check if the user has opted out.
     *
     * @return true if opted out, false otherwise
     */
    public fun isOptedOut(): Boolean {
        return platformIsOptedOut()
    }

    // ==================== Flush & Close ====================

    /**
     * Flush all queued events immediately.
     *
     * Events are normally batched and sent periodically. Call this
     * to force immediate delivery, for example before app close.
     */
    public fun flush() {
        platformFlush()
    }

    /**
     * Close the PostHog instance and release resources.
     *
     * Call this when your application is shutting down.
     */
    public fun close() {
        platformClose()
    }

    // ==================== Debug ====================

    /**
     * Enable or disable debug logging.
     *
     * @param enabled true to enable debug logs
     */
    public fun setDebug(enabled: Boolean) {
        platformSetDebug(enabled)
    }
}

/**
 * The configuration passed to [PostHog.setup]. Kept so per-call feature-flag
 * options can fall back to their configured values.
 */
@Volatile
internal var currentConfig: PostHogConfig? = null

private fun resolveSendFeatureFlagEvent(override: Boolean?): Boolean {
    return override ?: currentConfig?.sendFeatureFlagEvent ?: true
}

private fun Map<String, Any?>.dropNullValues(): Map<String, Any>? {
    return buildMap {
        this@dropNullValues.forEach { (key, value) -> if (value != null) put(key, value) }
    }.ifEmpty { null }
}

// ==================== Platform Expect Functions ====================

internal expect fun platformSetup(config: PostHogConfig, context: PostHogContext)

internal expect fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?
)

internal expect fun platformScreen(screenName: String, properties: Map<String, Any>?)

internal expect fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
)

internal expect fun platformAlias(alias: String)

internal expect fun platformReset()

internal expect fun platformGetDistinctId(): String?

internal expect fun platformRegister(key: String, value: Any)

internal expect fun platformUnregister(key: String)

internal expect fun platformSetAppLanguage(language: String?)

internal expect fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any>?
)

internal expect fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean

internal expect fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any?

internal expect fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult>

internal expect fun platformReloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)?)

internal expect fun platformOnFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription

internal expect fun platformSetPersonPropertiesForFlags(properties: Map<String, Any>, reloadFeatureFlags: Boolean)

internal expect fun platformResetPersonPropertiesForFlags(reloadFeatureFlags: Boolean)

internal expect fun platformSetGroupPropertiesForFlags(
    type: String,
    properties: Map<String, Any>,
    reloadFeatureFlags: Boolean
)

internal expect fun platformResetGroupPropertiesForFlags(type: String?, reloadFeatureFlags: Boolean)

internal expect fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult?

internal expect fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any>?
)

internal expect fun platformGetAnonymousId(): String?

internal expect fun platformGetSessionId(): String?

internal expect fun platformOptOut()

internal expect fun platformOptIn()

internal expect fun platformIsOptedOut(): Boolean

internal expect fun platformFlush()

internal expect fun platformClose()

internal expect fun platformSetDebug(enabled: Boolean)

internal expect fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>? = null
)
