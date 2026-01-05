package io.github.samuolis.posthog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Properties to set when identifying a user.
 *
 * @property distinctId Unique identifier for the user
 * @property userProperties Properties to set on the user profile
 * @property userPropertiesSetOnce Properties to set once (won't overwrite existing values)
 */
public data class IdentifyProperties(
    val distinctId: String,
    val userProperties: Map<String, Any?>? = null,
    val userPropertiesSetOnce: Map<String, Any?>? = null
)

/**
 * Group analytics properties for associating users with groups/organizations.
 *
 * @property type The group type (e.g., "company", "team", "project")
 * @property key The unique identifier for this group
 * @property groupProperties Optional properties to set on the group
 */
public data class GroupProperties(
    val type: String,
    val key: String,
    val groupProperties: Map<String, Any?>? = null
)

/**
 * Feature flag value that can be a boolean, string, or JSON payload.
 */
public sealed class FeatureFlagValue {
    /** Flag is disabled */
    public data object Disabled : FeatureFlagValue()

    /** Flag is enabled with boolean value */
    public data class Enabled(val value: Boolean = true) : FeatureFlagValue()

    /** Flag returns a string variant */
    public data class StringVariant(val variant: String) : FeatureFlagValue()

    /** Flag returns a JSON payload */
    public data class JsonPayload(val payload: Map<String, Any?>) : FeatureFlagValue()

    public val isEnabled: Boolean
        get() = when (this) {
            is Disabled -> false
            is Enabled -> value
            is StringVariant -> true
            is JsonPayload -> true
        }
}

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
 * Exception level for error tracking.
 */
public enum class ExceptionLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL
}

/**
 * Internal event types used by PostHog.
 */
public object PostHogEvents {
    public const val EXCEPTION: String = "\$exception"
    public const val SCREEN: String = "\$screen"
    public const val PAGEVIEW: String = "\$pageview"
    public const val PAGELEAVE: String = "\$pageleave"
    public const val IDENTIFY: String = "\$identify"
    public const val SET: String = "\$set"
    public const val SET_ONCE: String = "\$set_once"
    public const val UNSET: String = "\$unset"
    public const val GROUP_IDENTIFY: String = "\$groupidentify"
    public const val FEATURE_FLAG_CALLED: String = "\$feature_flag_called"
    public const val AUTOCAPTURE: String = "\$autocapture"
}

/**
 * Internal property keys used by PostHog.
 */
public object PostHogProperties {
    public const val DISTINCT_ID: String = "\$distinct_id"
    public const val SCREEN_NAME: String = "\$screen_name"
    public const val SCREEN_TITLE: String = "\$screen_title"
    public const val CURRENT_URL: String = "\$current_url"
    public const val HOST: String = "\$host"
    public const val PATHNAME: String = "\$pathname"
    public const val EXCEPTION_TYPE: String = "\$exception_type"
    public const val EXCEPTION_MESSAGE: String = "\$exception_message"
    public const val EXCEPTION_STACKTRACE: String = "\$exception_stacktrace"
    public const val EXCEPTION_LEVEL: String = "\$exception_level"
    public const val FEATURE_FLAG: String = "\$feature_flag"
    public const val FEATURE_FLAG_RESPONSE: String = "\$feature_flag_response"
    public const val GROUP_TYPE: String = "\$group_type"
    public const val GROUP_KEY: String = "\$group_key"
    public const val GROUP_SET: String = "\$group_set"
    public const val GROUPS: String = "\$groups"
    public const val ANON_DISTINCT_ID: String = "\$anon_distinct_id"
    public const val DEVICE_ID: String = "\$device_id"
    public const val SESSION_ID: String = "\$session_id"
    public const val LIB: String = "\$lib"
    public const val LIB_VERSION: String = "\$lib_version"
    public const val REFERRER: String = "\$referrer"
    public const val REFERRING_DOMAIN: String = "\$referring_domain"
}
