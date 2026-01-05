package com.samuolis.posthog

/**
 * Configuration options for PostHog SDK initialization.
 *
 * @property apiKey Your PostHog project API key (required)
 * @property host PostHog instance URL (defaults to US cloud)
 * @property debug Enable debug logging
 * @property captureApplicationLifecycleEvents Automatically capture app lifecycle events
 * @property captureScreenViews Automatically capture screen/page views
 * @property captureDeepLinks Capture deep link opens (Android/iOS only)
 * @property sendFeatureFlagEvent Send events when feature flags are evaluated
 * @property preloadFeatureFlags Preload feature flags on initialization
 * @property flushAt Number of events to batch before sending
 * @property flushIntervalSeconds Interval in seconds between automatic flushes
 * @property maxQueueSize Maximum number of events to queue
 * @property maxBatchSize Maximum events per batch
 * @property optOut Start with analytics opted out
 * @property personProfiles Person profile mode for feature flag targeting
 * @property sessionRecording Enable session recording (platform dependent)
 * @property autocapture Enable automatic event capture (platform dependent)
 */
public data class PostHogConfig(
    val apiKey: String,
    val host: String = "https://us.i.posthog.com",
    val debug: Boolean = false,
    val captureApplicationLifecycleEvents: Boolean = true,
    val captureScreenViews: Boolean = false,
    val captureDeepLinks: Boolean = true,
    val sendFeatureFlagEvent: Boolean = true,
    val preloadFeatureFlags: Boolean = true,
    val flushAt: Int = 20,
    val flushIntervalSeconds: Int = 30,
    val maxQueueSize: Int = 1000,
    val maxBatchSize: Int = 50,
    val optOut: Boolean = false,
    val personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY,
    val sessionRecording: SessionRecordingConfig? = null,
    val autocapture: Boolean = false
) {
    public companion object {
        /** PostHog US Cloud instance */
        public const val HOST_US: String = "https://us.i.posthog.com"

        /** PostHog EU Cloud instance */
        public const val HOST_EU: String = "https://eu.i.posthog.com"
    }
}

/**
 * Person profile modes for feature flag targeting and person data.
 */
public enum class PersonProfiles {
    /** Create person profiles for all users */
    ALWAYS,

    /** Only create profiles for identified users */
    IDENTIFIED_ONLY,

    /** Never create person profiles */
    NEVER
}

/**
 * Session recording configuration options.
 *
 * @property enabled Enable session recording
 * @property maskAllTextInputs Mask all text input values
 * @property maskAllImages Mask all images
 * @property captureNetworkTelemetry Include network requests in recording
 * @property captureLogs Capture console logs
 */
public data class SessionRecordingConfig(
    val enabled: Boolean = true,
    val maskAllTextInputs: Boolean = true,
    val maskAllImages: Boolean = false,
    val captureNetworkTelemetry: Boolean = true,
    val captureLogs: Boolean = true
)
