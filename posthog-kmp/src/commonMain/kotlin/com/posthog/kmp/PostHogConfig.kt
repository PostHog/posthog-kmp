package com.posthog.kmp

import kotlin.reflect.KClass

/**
 * Configuration options for PostHog SDK initialization.
 *
 * @property apiKey Your PostHog project API key (required)
 * @property host PostHog instance URL (defaults to US cloud)
 * @property debug Enable debug logging
 * @property captureApplicationLifecycleEvents Automatically capture app lifecycle events
 * @property captureScreenViews Automatically capture screen/page views
 * @property captureDeepLinks Capture deep link opens (Android only)
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
 * @property beforeSend Synchronous callbacks that can modify or drop events before they are queued
 * @property errorTracking Error tracking configuration (platform dependent)
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
    val autocapture: Boolean = false,
    val beforeSend: List<PostHogBeforeSend> = emptyList(),
    val errorTracking: ErrorTrackingConfig? = null
) {
    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        apiKey: String,
        host: String = "https://us.i.posthog.com",
        debug: Boolean = false,
        captureApplicationLifecycleEvents: Boolean = true,
        captureScreenViews: Boolean = false,
        captureDeepLinks: Boolean = true,
        sendFeatureFlagEvent: Boolean = true,
        preloadFeatureFlags: Boolean = true,
        flushAt: Int = 20,
        flushIntervalSeconds: Int = 30,
        maxQueueSize: Int = 1000,
        maxBatchSize: Int = 50,
        optOut: Boolean = false,
        personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY,
        sessionRecording: SessionRecordingConfig? = null,
        autocapture: Boolean = false,
        beforeSend: List<PostHogBeforeSend> = emptyList()
    ) : this(
        apiKey = apiKey,
        host = host,
        debug = debug,
        captureApplicationLifecycleEvents = captureApplicationLifecycleEvents,
        captureScreenViews = captureScreenViews,
        captureDeepLinks = captureDeepLinks,
        sendFeatureFlagEvent = sendFeatureFlagEvent,
        preloadFeatureFlags = preloadFeatureFlags,
        flushAt = flushAt,
        flushIntervalSeconds = flushIntervalSeconds,
        maxQueueSize = maxQueueSize,
        maxBatchSize = maxBatchSize,
        optOut = optOut,
        personProfiles = personProfiles,
        sessionRecording = sessionRecording,
        autocapture = autocapture,
        beforeSend = beforeSend,
        errorTracking = null
    )

    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        apiKey: String,
        host: String = "https://us.i.posthog.com",
        debug: Boolean = false,
        captureApplicationLifecycleEvents: Boolean = true,
        captureScreenViews: Boolean = false,
        captureDeepLinks: Boolean = true,
        sendFeatureFlagEvent: Boolean = true,
        preloadFeatureFlags: Boolean = true,
        flushAt: Int = 20,
        flushIntervalSeconds: Int = 30,
        maxQueueSize: Int = 1000,
        maxBatchSize: Int = 50,
        optOut: Boolean = false,
        personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY,
        sessionRecording: SessionRecordingConfig? = null,
        autocapture: Boolean = false
    ) : this(
        apiKey = apiKey,
        host = host,
        debug = debug,
        captureApplicationLifecycleEvents = captureApplicationLifecycleEvents,
        captureScreenViews = captureScreenViews,
        captureDeepLinks = captureDeepLinks,
        sendFeatureFlagEvent = sendFeatureFlagEvent,
        preloadFeatureFlags = preloadFeatureFlags,
        flushAt = flushAt,
        flushIntervalSeconds = flushIntervalSeconds,
        maxQueueSize = maxQueueSize,
        maxBatchSize = maxBatchSize,
        optOut = optOut,
        personProfiles = personProfiles,
        sessionRecording = sessionRecording,
        autocapture = autocapture,
        beforeSend = emptyList(),
        errorTracking = null
    )

    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        apiKey: String = this.apiKey,
        host: String = this.host,
        debug: Boolean = this.debug,
        captureApplicationLifecycleEvents: Boolean = this.captureApplicationLifecycleEvents,
        captureScreenViews: Boolean = this.captureScreenViews,
        captureDeepLinks: Boolean = this.captureDeepLinks,
        sendFeatureFlagEvent: Boolean = this.sendFeatureFlagEvent,
        preloadFeatureFlags: Boolean = this.preloadFeatureFlags,
        flushAt: Int = this.flushAt,
        flushIntervalSeconds: Int = this.flushIntervalSeconds,
        maxQueueSize: Int = this.maxQueueSize,
        maxBatchSize: Int = this.maxBatchSize,
        optOut: Boolean = this.optOut,
        personProfiles: PersonProfiles = this.personProfiles,
        sessionRecording: SessionRecordingConfig? = this.sessionRecording,
        autocapture: Boolean = this.autocapture,
        beforeSend: List<PostHogBeforeSend> = this.beforeSend
    ): PostHogConfig = PostHogConfig(
        apiKey = apiKey,
        host = host,
        debug = debug,
        captureApplicationLifecycleEvents = captureApplicationLifecycleEvents,
        captureScreenViews = captureScreenViews,
        captureDeepLinks = captureDeepLinks,
        sendFeatureFlagEvent = sendFeatureFlagEvent,
        preloadFeatureFlags = preloadFeatureFlags,
        flushAt = flushAt,
        flushIntervalSeconds = flushIntervalSeconds,
        maxQueueSize = maxQueueSize,
        maxBatchSize = maxBatchSize,
        optOut = optOut,
        personProfiles = personProfiles,
        sessionRecording = sessionRecording,
        autocapture = autocapture,
        beforeSend = beforeSend,
        errorTracking = errorTracking
    )

    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        apiKey: String = this.apiKey,
        host: String = this.host,
        debug: Boolean = this.debug,
        captureApplicationLifecycleEvents: Boolean = this.captureApplicationLifecycleEvents,
        captureScreenViews: Boolean = this.captureScreenViews,
        captureDeepLinks: Boolean = this.captureDeepLinks,
        sendFeatureFlagEvent: Boolean = this.sendFeatureFlagEvent,
        preloadFeatureFlags: Boolean = this.preloadFeatureFlags,
        flushAt: Int = this.flushAt,
        flushIntervalSeconds: Int = this.flushIntervalSeconds,
        maxQueueSize: Int = this.maxQueueSize,
        maxBatchSize: Int = this.maxBatchSize,
        optOut: Boolean = this.optOut,
        personProfiles: PersonProfiles = this.personProfiles,
        sessionRecording: SessionRecordingConfig? = this.sessionRecording,
        autocapture: Boolean = this.autocapture
    ): PostHogConfig = PostHogConfig(
        apiKey = apiKey,
        host = host,
        debug = debug,
        captureApplicationLifecycleEvents = captureApplicationLifecycleEvents,
        captureScreenViews = captureScreenViews,
        captureDeepLinks = captureDeepLinks,
        sendFeatureFlagEvent = sendFeatureFlagEvent,
        preloadFeatureFlags = preloadFeatureFlags,
        flushAt = flushAt,
        flushIntervalSeconds = flushIntervalSeconds,
        maxQueueSize = maxQueueSize,
        maxBatchSize = maxBatchSize,
        optOut = optOut,
        personProfiles = personProfiles,
        sessionRecording = sessionRecording,
        autocapture = autocapture,
        beforeSend = beforeSend,
        errorTracking = errorTracking
    )

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
 * Error tracking configuration options.
 *
 * @property autoCapture Automatically capture unhandled exceptions
 * @property inAppIncludes Additional package or bundle prefixes to mark as in-app frames (Android/JVM/iOS)
 * @property ignoredExceptionTypes Throwable types that should not be captured
 * @property inAppExcludes Package or bundle prefixes to mark as external frames (iOS only)
 * @property inAppByDefault Whether unmatched stack frames should be considered in-app (iOS only)
 *
 * TODO: Add exceptionSteps when PostHog.addExceptionStep is available in the common API.
 * TODO: Add support for configuring individual Web exception autocapture sources.
 */
public data class ErrorTrackingConfig(
    /**
     * Automatically capture unhandled exceptions.
     *
     * On Web, enabling this captures unhandled errors and unhandled promise rejections.
     * Console errors remain disabled.
     *
     * On Android, symbolication of minified frames requires the PostHog Android Gradle plugin
     * to upload the ProGuard or R8 mappings. On iOS, server-side symbolication requires uploading
     * the app's debug symbols (dSYMs).
     */
    val autoCapture: Boolean = false,
    /** Supported on Android, JVM, and iOS. */
    val inAppIncludes: List<String> = emptyList(),
    /**
     * Supported on Android, JVM, and iOS.
     *
     * Android and JVM match throwable classes through their type hierarchy. iOS matches simple class names,
     * so classes with the same name in different packages are treated as the same type. Native signal and
     * Mach exception types cannot be represented by [KClass] and are not filtered by this setting.
     */
    val ignoredExceptionTypes: List<KClass<out Throwable>> = emptyList(),
    /** iOS only. */
    val inAppExcludes: List<String> = emptyList(),
    /** iOS only. */
    val inAppByDefault: Boolean = true
)

/**
 * Session recording configuration options.
 *
 * Defaults match the native Android and iOS SDK defaults so wrapping adds no drift.
 *
 * @property enabled Enable session recording
 * @property maskAllTextInputs Mask all text input values
 * @property maskAllImages Mask all images
 * @property captureNetworkTelemetry Include network requests in recording
 * @property captureLogs Capture console logs (iOS/Web)
 * @property screenshot Enable screenshot mode instead of wireframe (Android/iOS experimental).
 *                       When enabled, the SDK takes actual screenshots instead of wireframe representations.
 *                       WARNING: Screenshots may contain sensitive information - ensure proper masking.
 * @property captureLogcat Capture Android logcat output (Android only)
 * @property debouncerDelayMs Delay in milliseconds for debouncing touch events (Android only)
 */
public data class SessionRecordingConfig(
    val enabled: Boolean = true,
    val maskAllTextInputs: Boolean = true,
    val maskAllImages: Boolean = true,
    val captureNetworkTelemetry: Boolean = true,
    val captureLogs: Boolean = false,
    val screenshot: Boolean = false,
    val captureLogcat: Boolean = true,
    val debouncerDelayMs: Long = 1000L
)
