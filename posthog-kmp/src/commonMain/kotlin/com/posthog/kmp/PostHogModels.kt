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
     * @return the payload as type T, or null if the payload is null or cast fails
     */
    public inline fun <reified T> getPayloadAs(): T? {
        return payload as? T
    }
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
    public const val FEATURE_FLAG_ERROR: String = "\$feature_flag_error"
    public const val AUTOCAPTURE: String = "\$autocapture"
    public const val SURVEY_SHOWN: String = "survey shown"
    public const val SURVEY_DISMISSED: String = "survey dismissed"
    public const val SURVEY_SENT: String = "survey sent"
    public const val WEB_VITALS: String = "\$web_vitals"
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
    public const val FEATURE_FLAG_ERROR: String = "\$feature_flag_error"
    public const val FEATURE_FLAG_REASON: String = "\$feature_flag_reason"
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
    public const val SURVEY_ID: String = "\$survey_id"
    public const val SURVEY_RESPONSE: String = "\$survey_response"
    public const val EVALUATED_AT: String = "\$evaluated_at"
    public const val WEB_VITALS_LCP: String = "\$web_vitals_LCP_value"
    public const val WEB_VITALS_CLS: String = "\$web_vitals_CLS_value"
    public const val WEB_VITALS_INP: String = "\$web_vitals_INP_value"
}
