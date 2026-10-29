package com.posthog.kmp

import kotlin.reflect.KClass

/**
 * Configuration options for PostHog SDK initialization.
 *
 * @property projectToken Your PostHog project token (required), which starts with `phc_`.
 *   Find it in your project settings: https://us.posthog.com/settings/project-details#variables.
 *   Formerly named `apiKey`. Example: `PostHogConfig(projectToken = "phc_your_project_token")`.
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
 * @property capturePushNotificationSubscriptions Auto-register the FCM token at startup when Firebase
 *   Messaging is available (Android only, defaults to true).
 * @property capturePushNotificationOpened Capture push notification opens automatically
 *   (Android only, defaults to true).
 */
public data class PostHogConfig(
    val projectToken: String,
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
    val errorTracking: ErrorTrackingConfig? = null,
    val capturePushNotificationSubscriptions: Boolean = true,
    val capturePushNotificationOpened: Boolean = true
) {
    /** Retains the constructor signature used by existing JVM consumers. */
    public constructor(
        projectToken: String,
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
        beforeSend: List<PostHogBeforeSend> = emptyList(),
        errorTracking: ErrorTrackingConfig? = null
    ) : this(
        projectToken = projectToken,
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
        errorTracking = errorTracking,
        capturePushNotificationSubscriptions = true,
        capturePushNotificationOpened = true
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
        autocapture: Boolean = false,
        beforeSend: List<PostHogBeforeSend> = emptyList()
    ) : this(
        projectToken = apiKey,
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
        projectToken = apiKey,
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

    /** Retains the copy signature used by existing JVM consumers. */
    public fun copy(
        projectToken: String = this.projectToken,
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
        beforeSend: List<PostHogBeforeSend> = this.beforeSend,
        errorTracking: ErrorTrackingConfig? = this.errorTracking
    ): PostHogConfig = PostHogConfig(
        projectToken = projectToken,
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
        errorTracking = errorTracking,
        capturePushNotificationSubscriptions = capturePushNotificationSubscriptions,
        capturePushNotificationOpened = capturePushNotificationOpened
    )

    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        apiKey: String = this.projectToken,
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
        projectToken = apiKey,
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
        errorTracking = errorTracking,
        capturePushNotificationSubscriptions = capturePushNotificationSubscriptions,
        capturePushNotificationOpened = capturePushNotificationOpened
    )

    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        apiKey: String = this.projectToken,
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
        projectToken = apiKey,
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
        errorTracking = errorTracking,
        capturePushNotificationSubscriptions = capturePushNotificationSubscriptions,
        capturePushNotificationOpened = capturePushNotificationOpened
    )

    /** Deprecated alias for [projectToken]. */
    @Deprecated(
        "Deprecated in favor of projectToken. This will be removed in the next major version.",
        ReplaceWith("projectToken")
    )
    val apiKey: String get() = projectToken

    public companion object {
        /** PostHog US Cloud instance */
        public const val HOST_US: String = "https://us.i.posthog.com"

        /** PostHog EU Cloud instance */
        public const val HOST_EU: String = "https://eu.i.posthog.com"

        @Deprecated(
            "Deprecated in favor of projectToken. This will be removed in the next major version."
        )
        public operator fun invoke(
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
            beforeSend: List<PostHogBeforeSend> = emptyList(),
            errorTracking: ErrorTrackingConfig? = null
        ): PostHogConfig = PostHogConfig(
            projectToken = apiKey,
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

        /** Deprecated named-argument form of the [PostHogConfig] constructor that takes `apiKey`. */
        @Deprecated(
            "Deprecated in favor of projectToken. This will be removed in the next major version."
        )
        public operator fun invoke(
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
            beforeSend: List<PostHogBeforeSend> = emptyList(),
            errorTracking: ErrorTrackingConfig? = null,
            capturePushNotificationSubscriptions: Boolean = true,
            capturePushNotificationOpened: Boolean = true
        ): PostHogConfig = PostHogConfig(
            projectToken = apiKey,
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
            errorTracking = errorTracking,
            capturePushNotificationSubscriptions = capturePushNotificationSubscriptions,
            capturePushNotificationOpened = capturePushNotificationOpened
        )
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
 * @property inAppExcludes Package or bundle prefixes to mark as external frames (iOS and macOS only)
 * @property inAppByDefault Whether unmatched stack frames should be considered in-app (iOS and macOS only)
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
    /** iOS and macOS only. */
    val inAppExcludes: List<String> = emptyList(),
    /** iOS and macOS only. */
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
 * @property captureLogcat Capture Android logcat output (Android only)
 * @property debouncerDelayMs Delay in milliseconds for debouncing touch events (Android only)
 * @property verifyScreenshotMaskAlignment Experimental screenshot mask-alignment verification (Android only).
 *   Can preserve captures during pixel-only redraws, but adds view-hierarchy walks during capture.
 *   Defaults to false and is ignored on other platforms. Windows with a Compose root always use
 *   verification regardless of this flag.
 */
public data class SessionRecordingConfig(
    val enabled: Boolean = true,
    val maskAllTextInputs: Boolean = true,
    val maskAllImages: Boolean = true,
    val captureNetworkTelemetry: Boolean = true,
    val captureLogs: Boolean = false,
    val captureLogcat: Boolean = true,
    val debouncerDelayMs: Long = 1000L,
    val verifyScreenshotMaskAlignment: Boolean = false
)
