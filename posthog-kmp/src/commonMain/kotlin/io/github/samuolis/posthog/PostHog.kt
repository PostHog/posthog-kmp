package io.github.samuolis.posthog

/**
 * PostHog Kotlin Multiplatform SDK
 *
 * A cross-platform analytics SDK for PostHog that works on Android, iOS, Web (JS/Wasm), and JVM.
 *
 * ## Basic Usage
 *
 * ```kotlin
 * // Initialize PostHog
 * PostHog.setup(PostHogConfig(apiKey = "phc_your_api_key"))
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

    private var isInitialized: Boolean = false

    private var config: PostHogConfig? = null
    private val superProperties = mutableMapOf<String, Any?>()

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
        if (isInitialized) {
            if (config.debug) {
                println("[PostHog] Already initialized, ignoring setup call")
            }
            return
        }

        this.config = config
        platformSetup(config, context)
        isInitialized = true

        if (config.debug) {
            println("[PostHog] Initialized with host: ${config.host}")
        }
    }

    /**
     * Initialize the PostHog SDK with the given configuration.
     *
     * On Android, you should prefer [setup] with context parameter to avoid memory leaks.
     * On other platforms, this is equivalent to calling setup(config, PostHogContext()).
     *
     * @param config PostHog configuration with your API key and options
     */
    public fun setup(config: PostHogConfig) {
        setup(config, PostHogContext())
    }

    /**
     * Check if PostHog SDK has been initialized.
     *
     * @return true if PostHog has been set up, false otherwise
     */
    public fun isSetup(): Boolean = isInitialized

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
        if (!isInitialized) return

        try {
            val mergedProperties = buildMap {
                superProperties.forEach { (key, value) -> put(key, value) }
                properties?.forEach { (key, value) -> put(key, value) }
                options?.groups?.let { put(PostHogProperties.GROUPS, it) }
            }.ifEmpty { null }

            platformCapture(event, mergedProperties)
        } catch (e: Throwable) {
            logError("capture", e)
        }
    }

    /**
     * Capture a screen/page view event.
     *
     * @param screenName The name of the screen or page
     * @param properties Optional additional properties
     */
    public fun screen(screenName: String, properties: Map<String, Any?>? = null) {
        if (!isInitialized) return

        try {
            platformScreen(screenName, properties)
        } catch (e: Throwable) {
            logError("screen", e)
        }
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
        if (!isInitialized) return

        try {
            platformIdentify(distinctId, userProperties, userPropertiesSetOnce)
        } catch (e: Throwable) {
            logError("identify", e)
        }
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
        if (!isInitialized) return

        try {
            platformAlias(alias)
        } catch (e: Throwable) {
            logError("alias", e)
        }
    }

    /**
     * Reset the current user's identity.
     *
     * Call this when a user logs out to clear their identity
     * and start tracking as an anonymous user again.
     */
    public fun reset() {
        if (!isInitialized) return

        try {
            superProperties.clear()
            platformReset()
        } catch (e: Throwable) {
            logError("reset", e)
        }
    }

    /**
     * Get the current user's distinct ID.
     *
     * @return The current distinct ID or null if not available
     */
    public fun getDistinctId(): String? {
        if (!isInitialized) return null

        return try {
            platformGetDistinctId()
        } catch (e: Throwable) {
            logError("getDistinctId", e)
            null
        }
    }

    // ==================== Super Properties ====================

    /**
     * Register a super property that will be sent with every event.
     *
     * @param key The property key
     * @param value The property value
     */
    public fun register(key: String, value: Any?) {
        if (!isInitialized) return

        try {
            superProperties[key] = value
            platformRegister(key, value)
        } catch (e: Throwable) {
            logError("register", e)
        }
    }

    /**
     * Register multiple super properties at once.
     *
     * @param properties Map of properties to register
     */
    public fun registerAll(properties: Map<String, Any?>) {
        properties.forEach { (key, value) -> register(key, value) }
    }

    /**
     * Unregister a super property.
     *
     * @param key The property key to remove
     */
    public fun unregister(key: String) {
        if (!isInitialized) return

        try {
            superProperties.remove(key)
            platformUnregister(key)
        } catch (e: Throwable) {
            logError("unregister", e)
        }
    }

    // ==================== Person Properties ====================

    /**
     * Set properties on the current person profile.
     *
     * @param properties Properties to set (will overwrite existing values)
     */
    public fun setPersonProperties(properties: Map<String, Any?>) {
        if (!isInitialized) return

        try {
            capture(PostHogEvents.SET, mapOf(PostHogEvents.SET to properties))
        } catch (e: Throwable) {
            logError("setPersonProperties", e)
        }
    }

    /**
     * Set properties on the current person profile only if they don't exist.
     *
     * @param properties Properties to set once
     */
    public fun setPersonPropertiesOnce(properties: Map<String, Any?>) {
        if (!isInitialized) return

        try {
            capture(PostHogEvents.SET_ONCE, mapOf(PostHogEvents.SET_ONCE to properties))
        } catch (e: Throwable) {
            logError("setPersonPropertiesOnce", e)
        }
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
        if (!isInitialized) return

        try {
            platformGroup(type, key, groupProperties)
        } catch (e: Throwable) {
            logError("group", e)
        }
    }

    // ==================== Feature Flags ====================

    /**
     * Check if a feature flag is enabled.
     *
     * @param key The feature flag key
     * @param defaultValue Value to return if the flag is not found (defaults to false)
     * @return true if the flag is enabled, defaultValue otherwise
     */
    public fun isFeatureEnabled(key: String, defaultValue: Boolean = false): Boolean {
        if (!isInitialized) return defaultValue

        return try {
            platformIsFeatureEnabled(key, defaultValue)
        } catch (e: Throwable) {
            logError("isFeatureEnabled", e)
            defaultValue
        }
    }

    /**
     * Get the value of a feature flag.
     *
     * Feature flags can return boolean, string variants, or JSON payloads.
     *
     * @param key The feature flag key
     * @return The flag value or null if not found
     */
    public fun getFeatureFlag(key: String): Any? {
        if (!isInitialized) return null

        return try {
            platformGetFeatureFlag(key)
        } catch (e: Throwable) {
            logError("getFeatureFlag", e)
            null
        }
    }

    /**
     * Get the JSON payload associated with a feature flag.
     *
     * @param key The feature flag key
     * @return The payload or null if not found
     */
    public fun getFeatureFlagPayload(key: String): Any? {
        if (!isInitialized) return null

        return try {
            platformGetFeatureFlagPayload(key)
        } catch (e: Throwable) {
            logError("getFeatureFlagPayload", e)
            null
        }
    }

    /**
     * Get all feature flags for the current user.
     *
     * @return Map of flag keys to their values
     */
    public fun getAllFeatureFlags(): Map<String, Any?> {
        if (!isInitialized) return emptyMap()

        return try {
            platformGetAllFeatureFlags()
        } catch (e: Throwable) {
            logError("getAllFeatureFlags", e)
            emptyMap()
        }
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
        if (!isInitialized) {
            callback?.invoke()
            return
        }

        try {
            platformReloadFeatureFlags(callback)
        } catch (e: Throwable) {
            logError("reloadFeatureFlags", e)
            callback?.invoke()
        }
    }

    /**
     * Override feature flags locally for testing.
     *
     * @param flags Map of flag keys to override values
     */
    public fun overrideFeatureFlags(flags: Map<String, Any?>) {
        if (!isInitialized) return

        try {
            platformOverrideFeatureFlags(flags)
        } catch (e: Throwable) {
            logError("overrideFeatureFlags", e)
        }
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
    public fun getFeatureFlagResult(key: String): FeatureFlagResult {
        if (!isInitialized) {
            return FeatureFlagResult(
                key = key,
                value = null,
                reason = FeatureFlagReason.ERROR,
                errorCode = FeatureFlagErrorCode.INVALID_CONFIG
            )
        }

        return try {
            platformGetFeatureFlagResult(key)
        } catch (e: Throwable) {
            logError("getFeatureFlagResult", e)
            FeatureFlagResult(
                key = key,
                value = null,
                reason = FeatureFlagReason.ERROR,
                errorCode = FeatureFlagErrorCode.UNKNOWN
            )
        }
    }

    // ==================== Session Management ====================

    /**
     * Get the current anonymous ID.
     *
     * The anonymous ID is a randomly generated identifier that persists
     * until the user is identified or the session is reset.
     *
     * @return The current anonymous ID or null if not available
     */
    public fun getAnonymousId(): String? {
        if (!isInitialized) return null

        return try {
            platformGetAnonymousId()
        } catch (e: Throwable) {
            logError("getAnonymousId", e)
            null
        }
    }

    /**
     * Get the current session ID.
     *
     * Session IDs are used to group events that occur within a single user session.
     *
     * @return The current session ID or null if not available
     */
    public fun getSessionId(): String? {
        if (!isInitialized) return null

        return try {
            platformGetSessionId()
        } catch (e: Throwable) {
            logError("getSessionId", e)
            null
        }
    }

    // ==================== Error Tracking ====================

    /**
     * Capture an exception for error tracking.
     *
     * @param throwable The exception to capture
     * @param level The severity level
     * @param additionalProperties Optional additional context
     */
    public fun captureException(
        throwable: Throwable,
        level: ExceptionLevel = ExceptionLevel.ERROR,
        additionalProperties: Map<String, Any?>? = null
    ) {
        if (!isInitialized) return

        try {
            val properties = buildMap {
                put(PostHogProperties.EXCEPTION_TYPE, throwable::class.simpleName ?: "Unknown")
                put(PostHogProperties.EXCEPTION_MESSAGE, throwable.message ?: "No message")
                put(PostHogProperties.EXCEPTION_STACKTRACE, throwable.stackTraceToString())
                put(PostHogProperties.EXCEPTION_LEVEL, level.name.lowercase())
                additionalProperties?.forEach { (key, value) -> put(key, value) }
            }

            capture(PostHogEvents.EXCEPTION, properties)
        } catch (e: Throwable) {
            logError("captureException", e)
        }
    }

    // ==================== Opt In/Out ====================

    /**
     * Opt out of analytics tracking.
     *
     * When opted out, no events will be captured or sent.
     */
    public fun optOut() {
        if (!isInitialized) return

        try {
            platformOptOut()
        } catch (e: Throwable) {
            logError("optOut", e)
        }
    }

    /**
     * Opt back into analytics tracking.
     */
    public fun optIn() {
        if (!isInitialized) return

        try {
            platformOptIn()
        } catch (e: Throwable) {
            logError("optIn", e)
        }
    }

    /**
     * Check if the user has opted out.
     *
     * @return true if opted out, false otherwise
     */
    public fun isOptedOut(): Boolean {
        if (!isInitialized) return false

        return try {
            platformIsOptedOut()
        } catch (e: Throwable) {
            logError("isOptedOut", e)
            false
        }
    }

    // ==================== Flush & Close ====================

    /**
     * Flush all queued events immediately.
     *
     * Events are normally batched and sent periodically. Call this
     * to force immediate delivery, for example before app close.
     */
    public fun flush() {
        if (!isInitialized) return

        try {
            platformFlush()
        } catch (e: Throwable) {
            logError("flush", e)
        }
    }

    /**
     * Close the PostHog instance and release resources.
     *
     * Call this when your application is shutting down.
     */
    public fun close() {
        if (!isInitialized) return

        try {
            platformClose()
            isInitialized = false
            config = null
            superProperties.clear()
        } catch (e: Throwable) {
            logError("close", e)
        }
    }

    // ==================== Debug ====================

    /**
     * Enable or disable debug logging.
     *
     * @param enabled true to enable debug logs
     */
    public fun setDebug(enabled: Boolean) {
        if (!isInitialized) return
        platformSetDebug(enabled)
    }

    // ==================== Internal ====================

    private fun logError(method: String, error: Throwable) {
        if (config?.debug == true) {
            println("[PostHog] Error in $method: ${error.message}")
        }
    }
}

// ==================== Platform Expect Functions ====================

internal expect fun platformSetup(config: PostHogConfig, context: PostHogContext)

internal expect fun platformCapture(event: String, properties: Map<String, Any?>?)

internal expect fun platformScreen(screenName: String, properties: Map<String, Any?>?)

internal expect fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
)

internal expect fun platformAlias(alias: String)

internal expect fun platformReset()

internal expect fun platformGetDistinctId(): String?

internal expect fun platformRegister(key: String, value: Any?)

internal expect fun platformUnregister(key: String)

internal expect fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
)

internal expect fun platformIsFeatureEnabled(key: String, defaultValue: Boolean): Boolean

internal expect fun platformGetFeatureFlag(key: String): Any?

internal expect fun platformGetFeatureFlagPayload(key: String): Any?

internal expect fun platformGetAllFeatureFlags(): Map<String, Any?>

internal expect fun platformReloadFeatureFlags(callback: (() -> Unit)?)

internal expect fun platformOverrideFeatureFlags(flags: Map<String, Any?>)

internal expect fun platformGetFeatureFlagResult(key: String): FeatureFlagResult

internal expect fun platformGetAnonymousId(): String?

internal expect fun platformGetSessionId(): String?

internal expect fun platformOptOut()

internal expect fun platformOptIn()

internal expect fun platformIsOptedOut(): Boolean

internal expect fun platformFlush()

internal expect fun platformClose()

internal expect fun platformSetDebug(enabled: Boolean)
