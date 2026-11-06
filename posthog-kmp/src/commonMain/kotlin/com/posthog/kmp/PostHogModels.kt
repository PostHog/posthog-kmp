package com.posthog.kmp

/**
 * Options for capturing events.
 *
 * @property groups Groups to associate with this event
 * @property timestamp Custom event time as Unix epoch milliseconds, representing a UTC instant
 *   (defaults to now). UTC is preferred when deriving this value from a date and time; resolve any
 *   non-UTC offset to the equivalent instant before converting it to epoch milliseconds. Platform
 *   bridges preserve this instant when constructing their native date type.
 */
public data class CaptureOptions(
    val groups: Map<String, String>? = null,
    val timestamp: Long? = null
)

/**
 * Result of a feature flag evaluation with detailed information.
 *
 * @property key the key of the feature flag
 * @property enabled whether the feature flag is enabled
 * @property variant the variant of the feature flag (null for boolean flags)
 * @property payload Optional JSON payload associated with the flag
 */
public data class FeatureFlagResult(
    public val key: String,
    public val enabled: Boolean,
    public val variant: String? = null,
    public val payload: Any? = null,
) {
    /**
     * Returns the effective value of the feature flag.
     * For multivariate flags, returns the variant string.
     * For boolean flags, returns the enabled boolean.
     */
    public val value: Any
        get() = variant ?: enabled

    /**
     * Returns the payload cast as the specified type.
     *
     * This is a direct runtime cast, not a deserialization: it succeeds only when the
     * payload is already an instance of [T] (e.g. a primitive, `String`, `Map`, or `List`).
     * It does not deserialize a JSON payload into a custom data class — for that, decode
     * [payload] yourself (e.g. with kotlinx.serialization).
     *
     * @return the payload as type [T], or null if the payload is null or is not a [T]
     */
    public inline fun <reified T> getPayloadAs(): T? {
        return payload as? T
    }
}

/**
 * The feature flags passed to [PostHog.onFeatureFlags] and [PostHog.reloadFeatureFlags] callbacks.
 *
 * It holds the enabled flags as they were when the callback was invoked, and whether the latest
 * attempt to load them failed. Disabled flags are left out; read them with [PostHog.getFeatureFlag].
 *
 * Example:
 * ```kotlin
 * PostHog.reloadFeatureFlags { loaded ->
 *     if (loaded.errorsLoading) {
 *         // The values are the last known (cached) flags
 *     }
 *     if (loaded.variants["new-checkout"] == "test") {
 *         // Show the test variant
 *     }
 * }
 * ```
 */
public class PostHogFeatureFlagsLoaded internal constructor(
    featureFlags: Map<String, Any?>,
    /**
     * `true` if the latest attempt to load feature flags failed.
     *
     * When it did, [flags] and [variants] hold the last known values, which can be empty. It's also
     * `true`, with no values, when [PostHog.reloadFeatureFlags] is called before [PostHog.setup].
     */
    public val errorsLoading: Boolean,
) {
    /**
     * The values of the enabled feature flags, keyed by flag key.
     *
     * A value is `true` for an enabled boolean flag, or the variant `String` for a multivariate flag.
     * A flag whose variant is an empty string counts as disabled and is left out.
     */
    public val variants: Map<String, Any> = featureFlags.entries
        .mapNotNull { (key, value) -> if (value == true || (value is String && value.isNotEmpty())) key to value else null }
        .toMap()

    /** The keys of the enabled feature flags. */
    public val flags: List<String> = variants.keys.toList()

    override fun toString(): String = "PostHogFeatureFlagsLoaded(variants=$variants, errorsLoading=$errorsLoading)"
}

/**
 * A registration returned by [PostHog.onFeatureFlags].
 *
 * The callback stays registered, including across [PostHog.close] and [PostHog.setup], until you
 * call [unsubscribe]. Keep a reference only if you want to unsubscribe later.
 *
 * Example:
 * ```kotlin
 * val subscription = PostHog.onFeatureFlags { loaded -> render(loaded.variants) }
 * // Later, stop listening
 * subscription.unsubscribe()
 * ```
 */
public class PostHogFeatureFlagsSubscription internal constructor(
    private val onUnsubscribe: () -> Unit,
) {
    /** Stops the callback from being invoked. Calling it more than once has no effect. */
    public fun unsubscribe() {
        onUnsubscribe()
    }
}

/**
 * Builds a [FeatureFlagResult] from a platform result. An empty variant reads as disabled on every
 * platform, the same as [PostHog.isFeatureEnabled].
 */
internal fun featureFlagResult(key: String, enabled: Boolean, variant: String?, payload: Any?): FeatureFlagResult =
    FeatureFlagResult(key = key, enabled = enabled && variant != "", variant = variant, payload = payload)

/**
 * Internal property keys used by PostHog.
 */
internal object PostHogProperties {
    internal const val GROUPS: String = "\$groups"
}
