package com.posthog.kmp

/**
 * PostHog Kotlin Multiplatform SDK
 *
 * A cross-platform analytics SDK for PostHog that works on Android, iOS, Web (JS/Wasm), and JVM.
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Initialize PostHog
 * PostHog.setup(PostHogConfig(apiKey = "phc_your_api_key"), PostHogContext())
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
        platformSetup(config, context)
    }

    // ==================== Event Capture ====================

    /**
     * Capture an event with optional properties.
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
        val mergedProperties = buildMap {
            properties?.forEach { (key, value) -> put(key, value) }
            options?.groups?.let { put(PostHogProperties.GROUPS, it) }
        }.ifEmpty { null }

        platformCapture(event, mergedProperties, options?.timestamp)
    }

    /**
     * Capture a screen/page view event.
     *
     * @param screenName The name of the screen or page
     * @param properties Optional additional properties
     */
    public fun screen(screenName: String, properties: Map<String, Any?>? = null) {
        platformScreen(screenName, properties)
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
        platformIdentify(distinctId, userProperties, userPropertiesSetOnce)
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
        platformGroup(type, key, groupProperties)
    }

    // ==================== Feature Flags ====================

    /**
     * Check if a feature flag is enabled.
     *
     * @param key The feature flag key
     * @param defaultValue Value to return if the flag is not found (defaults to false)
     * @return true if the flag is enabled, defaultValue otherwise
     */
    public fun isFeatureEnabled(key: String, defaultValue: Boolean = false, sendFeatureFlagEvent: Boolean = true): Boolean {
        return platformIsFeatureEnabled(key, defaultValue, sendFeatureFlagEvent)
    }

    /**
     * Get the value of a feature flag.
     *
     * Feature flags can return boolean, string variants, or JSON payloads.
     *
     * @param key The feature flag key
     * @return The flag value or null if not found
     */
    public fun getFeatureFlag(key: String, sendFeatureFlagEvent: Boolean  = true): Any? {
        return platformGetFeatureFlag(key, sendFeatureFlagEvent)
    }

    /**
     * Get all feature flags for the current user.
     * Note: Android and iOS just returns empty map, will be supported in the next version.
     * @return Map of flag keys to their values
     */
    public fun getAllFeatureFlags(): Map<String, Any?> {
        return platformGetAllFeatureFlags()
    }

    /**
     * Reload feature flags from the server.
     *
     * Call this to get fresh feature flag values, for example after
     * updating user properties that affect flag targeting.
     *
     * @param callback Optional callback when flags are loaded
     */
    public fun reloadFeatureFlags(callback: (() -> Unit)? = null) {
        platformReloadFeatureFlags(callback)
    }

    /**
     * Get detailed information about a feature flag evaluation.
     *
     * This method provides more information than [getFeatureFlag], including
     * the reason for the evaluation result and any associated payload.
     *
     * @param key The feature flag key
     * @return A [FeatureFlagResult] with detailed evaluation information
     */
    public fun getFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean = true): FeatureFlagResult? {
        return platformGetFeatureFlagResult(key, sendFeatureFlagEvent)
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
        platformCaptureException(throwable, additionalProperties)
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

// ==================== Platform Expect Functions ====================

internal expect fun platformSetup(config: PostHogConfig, context: PostHogContext)

internal expect fun platformCapture(event: String, properties: Map<String, Any?>?, timestamp: Long?)

internal expect fun platformScreen(screenName: String, properties: Map<String, Any?>?)

internal expect fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
)

internal expect fun platformAlias(alias: String)

internal expect fun platformReset()

internal expect fun platformGetDistinctId(): String?

internal expect fun platformRegister(key: String, value: Any)

internal expect fun platformUnregister(key: String)

internal expect fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
)

internal expect fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean

internal expect fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any?

internal expect fun platformGetAllFeatureFlags(): Map<String, Any?>

internal expect fun platformReloadFeatureFlags(callback: (() -> Unit)?)

internal expect fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult?

internal expect fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any?>?
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
