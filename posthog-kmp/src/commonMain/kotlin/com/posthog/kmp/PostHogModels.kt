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
    val timestamp: Long? = null,
    /** Settings that tell PostHog how to process this event. See [PostHogEventOptions]. */
    val eventOptions: Map<String, Any?>? = null
)

/**
 * Keys for event options: settings that tell PostHog how to process an event, sent apart from its
 * properties. Pass them in a map, such as
 * `CaptureOptions(eventOptions = mapOf(PostHogEventOptions.PROCESS_PERSON_PROFILE to false))`.
 *
 * Options are sent as given, so a key not listed here, such as one a newer PostHog version reads,
 * also works. A `null` value is left unset, so PostHog and the SDK use their defaults.
 *
 * These replace the `$process_person_profile`, `$cookieless_mode`, `$ignore_sent_at` and
 * `$product_tour_id` properties. Those properties still work, and an option wins over its
 * property.
 *
 * On JavaScript and Wasm, posthog-js has no options, so the keys listed here are sent as those
 * properties on [PostHog.capture], [PostHog.screen] and [PostHog.captureException] events, and
 * dropped by [PostHog.identify], [PostHog.alias], [PostHog.group] and [PostHog.setPersonProperties],
 * which posthog-js sends without event properties. Other keys are dropped there. posthog-js sets
 * `$process_person_profile` itself, so [PROCESS_PERSON_PROFILE] has no effect there.
 */
public object PostHogEventOptions {
    /** Whether PostHog creates or updates a person profile for the event. A `Boolean`. */
    public const val PROCESS_PERSON_PROFILE: String = "process_person_profile"

    /** Whether PostHog processes the event in cookieless mode. A `Boolean`. */
    public const val COOKIELESS_MODE: String = "cookieless_mode"

    /**
     * Whether PostHog keeps the event timestamp as sent instead of correcting it for device clock
     * skew. A `Boolean`.
     */
    public const val DISABLE_SKEW_CORRECTION: String = "disable_skew_correction"

    /** The product tour the event belongs to. A `String`. */
    public const val PRODUCT_TOUR_ID: String = "product_tour_id"
}

/** Each option key and the legacy property posthog-js reads instead. */
internal val LEGACY_OPTION_PROPERTIES: Map<String, String> = mapOf(
    PostHogEventOptions.PROCESS_PERSON_PROFILE to "\$process_person_profile",
    PostHogEventOptions.COOKIELESS_MODE to "\$cookieless_mode",
    PostHogEventOptions.DISABLE_SKEW_CORRECTION to "\$ignore_sent_at",
    PostHogEventOptions.PRODUCT_TOUR_ID to "\$product_tour_id"
)

/** The known options as the legacy properties posthog-js reads; `null` when there are none. */
internal fun Map<String, Any>?.toLegacyProperties(): Map<String, Any>? = this
    ?.mapNotNull { (key, value) -> LEGACY_OPTION_PROPERTIES[key]?.let { it to value } }
    ?.toMap()
    ?.ifEmpty { null }

/** [properties] with the [options] set as their legacy properties, which win over the same keys. */
internal fun withLegacyOptionProperties(properties: Map<String, Any>?, options: Map<String, Any>?): Map<String, Any>? {
    val legacy = options.toLegacyProperties() ?: return properties
    return properties.orEmpty() + legacy
}

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
