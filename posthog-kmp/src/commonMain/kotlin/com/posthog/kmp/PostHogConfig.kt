package com.posthog.kmp

import kotlin.reflect.KClass

/**
 * Configuration options for PostHog SDK initialization.
 *
 * @property projectToken Your PostHog project token (required), which starts with `phc_`.
 *   Find it in your project settings: https://us.posthog.com/settings/project-details#variables.
 *   Example: `PostHogConfig(projectToken = "phc_your_project_token")`.
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
 * @property surveys Load surveys and show the ones that match (defaults to true). Set to false to keep
 *   the SDK from loading or showing surveys. Applies on iOS 15+ and web. On Android, surveys display only
 *   when the app also depends on `com.posthog:posthog-android-surveys-compose`. Ignored on macOS and JVM.
 * @property evaluationContexts Evaluation context tags that limit which feature flags are evaluated
 *   (defaults to null, which evaluates every flag). When set, PostHog evaluates only the flags that
 *   have at least one of these evaluation tags, plus flags with no evaluation tags. Example:
 *   `PostHogConfig(projectToken = "phc_...", evaluationContexts = listOf("production", "checkout"))`.
 * @property captureAiCompression How [PostHog.captureAi] request bodies are compressed (defaults to
 *   null, which uses the native SDK's default, [PostHogCompression.GZIP]). Other requests aren't
 *   affected. Android, JVM, iOS and macOS only.
 * @property captureAiMaxQueueSize Maximum number of [PostHog.captureAi] events kept in their
 *   on-disk queue before the oldest are dropped (defaults to null, which uses the native SDK's
 *   default, 100). The queue also keeps at most 50 MiB of AI events. A value of 0 or less uses the
 *   default. Android, JVM, iOS and macOS only.
 * @property captureAiTimeout How long, in seconds, a [PostHog.captureAi] request can go without
 *   network activity before it times out (defaults to null, which uses the native SDK's default,
 *   60). A value of 0 or less uses the default. Android, JVM, iOS and macOS only.
 * @property captureAiMaxEventBytes Maximum size, in bytes, of a serialized [PostHog.captureAi]
 *   event (defaults to null, which uses the native SDK's default, 8 MiB plus 64 KiB). The SDK drops
 *   a larger event, and `captureAi` returns `null`. It can only be lowered: a larger value uses the
 *   default and logs a warning. A value of 0 or less uses the default. Android, JVM, iOS and macOS
 *   only.
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
    val capturePushNotificationOpened: Boolean = true,
    val surveys: Boolean = true,
    val evaluationContexts: List<String>? = null,
    val captureAiCompression: PostHogCompression? = null,
    val captureAiMaxQueueSize: Int? = null,
    val captureAiTimeout: Int? = null,
    val captureAiMaxEventBytes: Int? = null
) {
    public companion object {
        /** PostHog US Cloud instance */
        public const val HOST_US: String = "https://us.i.posthog.com"

        /** PostHog EU Cloud instance */
        public const val HOST_EU: String = "https://eu.i.posthog.com"
    }
}

/**
 * How the SDK compresses request bodies.
 */
public enum class PostHogCompression {
    /** Gzip request bodies */
    GZIP,

    /** Send request bodies uncompressed */
    NONE
}

/**
 * How iOS captures each session replay screenshot.
 */
public enum class IosCaptureMethod {
    /** Draws the window on the main thread, once the screen has settled. The default. */
    STANDARD,

    /**
     * Experimental. Copies the window's layers and renders the copy on the GPU, in short
     * main-thread steps. Content from another process, such as a web view's page, is not captured,
     * and Metal content, including Compose Multiplatform screens, is recorded as a placeholder.
     * Falls back to [STANDARD] when a capture can't be made this way.
     */
    GPU
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
 * @property throttleDelayMs Minimum delay in milliseconds between replay snapshots (Android and iOS)
 * @property verifyScreenshotMaskAlignment Experimental screenshot mask-alignment verification (Android only).
 *   Can preserve captures during pixel-only redraws, but adds view-hierarchy walks during capture.
 *   Defaults to false and is ignored on other platforms. Windows with a Compose root always use
 *   verification regardless of this flag.
 * @property iosCaptureMethod How iOS captures each replay screenshot (iOS only). Defaults to
 *   [IosCaptureMethod.STANDARD]; set the experimental [IosCaptureMethod.GPU] for less main-thread
 *   time on screens without Compose Multiplatform, web views or Metal content. Ignored on other platforms.
 */
public data class SessionRecordingConfig(
    val enabled: Boolean = true,
    val maskAllTextInputs: Boolean = true,
    val maskAllImages: Boolean = true,
    val captureNetworkTelemetry: Boolean = true,
    val captureLogs: Boolean = false,
    val captureLogcat: Boolean = true,
    val throttleDelayMs: Long = 1000L,
    val verifyScreenshotMaskAlignment: Boolean = false,
    val iosCaptureMethod: IosCaptureMethod = IosCaptureMethod.STANDARD
)
