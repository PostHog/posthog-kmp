package com.posthog.kmp

/**
 * Options for capturing events.
 *
 * @property groups Groups to associate with this event
 * @property timestamp Custom timestamp for the event (defaults to now)
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
 * Internal property keys used by PostHog.
 */
internal object PostHogProperties {
    internal const val GROUPS: String = "\$groups"
}
