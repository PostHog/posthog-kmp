@file:JvmName("PostHogConfigs")

package com.posthog.kmp

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/** Marks the PostHog configuration builders, so a nested block can't set an outer block's options by accident. */
@DslMarker
public annotation class PostHogDsl

/**
 * Creates the configuration for [PostHog.setup].
 *
 * ```kotlin
 * val config = PostHogConfig("phc_your_project_token") {
 *     host = PostHogConfig.HOST_EU
 *     sessionReplay { maskAllImages = false }
 * }
 * ```
 *
 * @param projectToken Your PostHog project token, which starts with `phc_`. Find it in your project settings:
 *   https://us.posthog.com/settings/project-details#variables.
 * @param configure Sets the options. See [PostHogConfig.Builder] for each one and its default.
 */
public fun PostHogConfig(projectToken: String, configure: PostHogConfig.Builder.() -> Unit = {}): PostHogConfig =
    PostHogConfig.Builder(projectToken).apply(configure).build()

/**
 * Configuration for [PostHog.setup]. Create it with the [PostHogConfig] function. Each property holds the
 * [PostHogConfig.Builder] option of the same name.
 */
public class PostHogConfig private constructor(builder: Builder) {
    public val projectToken: String = builder.projectToken
    public val host: String = builder.host
    public val debug: Boolean = builder.debug
    public val captureApplicationLifecycleEvents: Boolean = builder.captureApplicationLifecycleEvents
    public val captureScreenViews: Boolean = builder.captureScreenViews
    public val captureDeepLinks: Boolean = builder.captureDeepLinks
    public val sendFeatureFlagEvent: Boolean = builder.sendFeatureFlagEvent
    public val preloadFeatureFlags: Boolean = builder.preloadFeatureFlags
    public val flushAt: Int = builder.flushAt
    public val flushIntervalSeconds: Int = builder.flushIntervalSeconds
    public val maxQueueSize: Int = builder.maxQueueSize
    public val maxBatchSize: Int = builder.maxBatchSize
    public val optOut: Boolean = builder.optOut
    public val personProfiles: PersonProfiles = builder.personProfiles
    public val sessionReplay: SessionReplayConfig? = builder.sessionReplayConfig
    public val autocapture: Boolean = builder.autocapture
    public val beforeSend: List<PostHogBeforeSend> = builder.beforeSend.toList()
    public val errorTracking: ErrorTrackingConfig? = builder.errorTrackingConfig
    public val capturePushNotificationSubscriptions: Boolean = builder.capturePushNotificationSubscriptions
    public val capturePushNotificationOpened: Boolean = builder.capturePushNotificationOpened
    public val surveys: Boolean = builder.surveys
    public val evaluationContexts: List<String>? = builder.evaluationContexts?.toList()
    public val captureAiCompression: PostHogCompression? = builder.captureAiCompression
    public val captureAiMaxQueueSize: Int? = builder.captureAiMaxQueueSize
    public val captureAiTimeout: Int? = builder.captureAiTimeout
    public val captureAiMaxEventBytes: Int? = builder.captureAiMaxEventBytes

    /** Sets the options of a [PostHogConfig]. */
    @PostHogDsl
    public class Builder internal constructor(internal val projectToken: String) {
        /** PostHog instance URL. Defaults to [HOST_US]. */
        public var host: String = HOST_US

        /** Enable debug logging. Defaults to false. */
        public var debug: Boolean = false

        /** Automatically capture app lifecycle events. Defaults to true. */
        public var captureApplicationLifecycleEvents: Boolean = true

        /**
         * Automatically capture screen and page views. Defaults to true, like the native SDKs. Android sends a
         * `$screen` event with the Activity's label when it starts, iOS sends one when a view controller appears,
         * and web sends `$pageview` and `$pageleave`. A Compose Multiplatform app usually has one Activity or view
         * controller, so call [PostHog.screen] for each Compose screen. Ignored on macOS and JVM.
         */
        public var captureScreenViews: Boolean = true

        /** Capture deep link opens (Android only). Defaults to true. */
        public var captureDeepLinks: Boolean = true

        /** Send `$feature_flag_called` events when feature flags are evaluated. Defaults to true. */
        public var sendFeatureFlagEvent: Boolean = true

        /** Load feature flags at setup. Defaults to true. */
        public var preloadFeatureFlags: Boolean = true

        /** Number of queued events that triggers a send. Defaults to 20. */
        public var flushAt: Int = 20

        /** Interval in seconds between automatic sends. Defaults to 30. */
        public var flushIntervalSeconds: Int = 30

        /** Maximum number of events to queue. Defaults to 1000. */
        public var maxQueueSize: Int = 1000

        /** Maximum number of events per request. Defaults to 50. */
        public var maxBatchSize: Int = 50

        /** Start with capture opted out. Defaults to false. */
        public var optOut: Boolean = false

        /** When to create person profiles. Defaults to [PersonProfiles.IDENTIFIED_ONLY]. */
        public var personProfiles: PersonProfiles = PersonProfiles.IDENTIFIED_ONLY

        /** Enable automatic event capture (platform dependent). Defaults to false. */
        public var autocapture: Boolean = false

        /** Synchronous callbacks that can change or drop events before they are queued, run in order. */
        public var beforeSend: List<PostHogBeforeSend> = emptyList()

        /**
         * Auto-register the FCM token at startup when Firebase Messaging is available (Android only). Defaults
         * to true.
         */
        public var capturePushNotificationSubscriptions: Boolean = true

        /** Capture push notification opens automatically (Android only). Defaults to true. */
        public var capturePushNotificationOpened: Boolean = true

        /**
         * Load surveys and show the ones that match. Defaults to true. Set to false to keep the SDK from loading
         * or showing surveys. Applies on iOS 15+ and web. On Android, surveys display only when the app also
         * depends on `com.posthog:posthog-android-surveys-compose`. Ignored on macOS and JVM.
         */
        public var surveys: Boolean = true

        /**
         * Evaluation context tags that limit which feature flags are evaluated. Defaults to null, which evaluates
         * every flag. When set, PostHog evaluates only the flags that have at least one of these evaluation tags,
         * plus flags with no evaluation tags.
         */
        public var evaluationContexts: List<String>? = null

        /**
         * How [PostHog.captureAi] request bodies are compressed. Defaults to null, which uses the native SDK's
         * default, [PostHogCompression.GZIP]. Other requests aren't affected. Android, JVM, iOS and macOS only.
         */
        public var captureAiCompression: PostHogCompression? = null

        /**
         * Maximum number of [PostHog.captureAi] events kept in their on-disk queue before the oldest are
         * dropped. Defaults to null, which uses the native SDK's default, 100. The queue also keeps at most
         * 50 MiB of AI events. A value of 0 or less uses the default. Android, JVM, iOS and macOS only.
         */
        public var captureAiMaxQueueSize: Int? = null

        /**
         * How long, in seconds, a [PostHog.captureAi] request can go without network activity before it times
         * out. Defaults to null, which uses the native SDK's default, 60. A value of 0 or less uses the default.
         * Android, JVM, iOS and macOS only.
         */
        public var captureAiTimeout: Int? = null

        /**
         * Maximum size, in bytes, of a serialized [PostHog.captureAi] event. Defaults to null, which uses the
         * native SDK's default, 8 MiB plus 64 KiB. The SDK drops a larger event, and `captureAi` returns `null`.
         * It can only be lowered: a larger value uses the default and logs a warning. A value of 0 or less uses
         * the default. Android, JVM, iOS and macOS only.
         */
        public var captureAiMaxEventBytes: Int? = null

        internal var sessionReplayConfig: SessionReplayConfig? = null
        internal var errorTrackingConfig: ErrorTrackingConfig? = null

        /** Turns on session replay (Android and iOS) with the given options. Off unless you call this. */
        @JvmOverloads
        public fun sessionReplay(configure: SessionReplayConfig.Builder.() -> Unit = {}) {
            sessionReplayConfig = SessionReplayConfig(configure)
        }

        /** Configures error tracking with the given options. */
        @JvmOverloads
        public fun errorTracking(configure: ErrorTrackingConfig.Builder.() -> Unit = {}) {
            errorTrackingConfig = ErrorTrackingConfig(configure)
        }

        internal fun build(): PostHogConfig = PostHogConfig(this)
    }

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

internal fun ErrorTrackingConfig(configure: ErrorTrackingConfig.Builder.() -> Unit = {}): ErrorTrackingConfig =
    ErrorTrackingConfig.Builder().apply(configure).build()

/**
 * Error tracking options, set with [PostHogConfig.Builder.errorTracking]. Each property holds the
 * [ErrorTrackingConfig.Builder] option of the same name.
 *
 * TODO: Add exceptionSteps when PostHog.addExceptionStep is available in the common API.
 * TODO: Add support for configuring individual Web exception autocapture sources.
 */
public class ErrorTrackingConfig private constructor(builder: Builder) {
    public val autoCapture: Boolean = builder.autoCapture
    public val inAppIncludes: List<String> = builder.inAppIncludes.toList()
    public val ignoredExceptionTypes: List<KClass<out Throwable>> = builder.ignoredExceptionTypes.toList()
    public val inAppExcludes: List<String> = builder.inAppExcludes.toList()
    public val inAppByDefault: Boolean = builder.inAppByDefault

    /** Sets the options of an [ErrorTrackingConfig]. */
    @PostHogDsl
    public class Builder internal constructor() {
        /**
         * Automatically capture unhandled exceptions. Defaults to false.
         *
         * On Web, enabling this captures unhandled errors and unhandled promise rejections.
         * Console errors remain disabled.
         *
         * On Android, symbolication of minified frames requires the PostHog Android Gradle plugin
         * to upload the ProGuard or R8 mappings. On iOS, server-side symbolication requires uploading
         * the app's debug symbols (dSYMs).
         */
        public var autoCapture: Boolean = false

        /** Additional package or bundle prefixes to mark as in-app frames. Android, JVM, and iOS. */
        public var inAppIncludes: List<String> = emptyList()

        /**
         * Throwable types that should not be captured. Android, JVM, and iOS.
         *
         * Android and JVM match throwable classes through their type hierarchy. iOS matches simple class names,
         * so classes with the same name in different packages are treated as the same type. Native signal and
         * Mach exception types cannot be represented by [KClass] and are not filtered by this setting.
         */
        public var ignoredExceptionTypes: List<KClass<out Throwable>> = emptyList()

        /** Package or bundle prefixes to mark as external frames. iOS and macOS only. */
        public var inAppExcludes: List<String> = emptyList()

        /** Whether unmatched stack frames are considered in-app. Defaults to true. iOS and macOS only. */
        public var inAppByDefault: Boolean = true

        internal fun build(): ErrorTrackingConfig = ErrorTrackingConfig(this)
    }
}

internal fun SessionReplayConfig(configure: SessionReplayConfig.Builder.() -> Unit = {}): SessionReplayConfig =
    SessionReplayConfig.Builder().apply(configure).build()

/**
 * Session replay options, set with [PostHogConfig.Builder.sessionReplay]. Each property holds the
 * [SessionReplayConfig.Builder] option of the same name. Defaults match the native Android and iOS SDKs.
 */
public class SessionReplayConfig private constructor(builder: Builder) {
    public val enabled: Boolean = builder.enabled
    public val maskAllTextInputs: Boolean = builder.maskAllTextInputs
    public val maskAllImages: Boolean = builder.maskAllImages
    public val captureNetworkTelemetry: Boolean = builder.captureNetworkTelemetry
    public val captureLogs: Boolean = builder.captureLogs
    public val captureLogcat: Boolean = builder.captureLogcat
    public val throttleDelayMs: Long = builder.throttleDelayMs
    public val verifyScreenshotMaskAlignment: Boolean = builder.verifyScreenshotMaskAlignment
    public val iosCaptureMethod: IosCaptureMethod = builder.iosCaptureMethod

    /** Sets the options of a [SessionReplayConfig]. */
    @PostHogDsl
    public class Builder internal constructor() {
        /** Record sessions. Defaults to true. */
        public var enabled: Boolean = true

        /** Mask all text input values. Defaults to true. */
        public var maskAllTextInputs: Boolean = true

        /** Mask all images. Defaults to true. */
        public var maskAllImages: Boolean = true

        /** Include network requests in session replay. Defaults to true. */
        public var captureNetworkTelemetry: Boolean = true

        /** Capture console logs (iOS and web). Defaults to false. */
        public var captureLogs: Boolean = false

        /** Capture Android logcat output (Android only). Defaults to true. */
        public var captureLogcat: Boolean = true

        /** Minimum delay in milliseconds between replay snapshots (Android and iOS). Defaults to 1000. */
        public var throttleDelayMs: Long = 1000L

        /**
         * Experimental screenshot mask-alignment verification (Android only). Defaults to false. Can preserve
         * captures during pixel-only redraws, but adds view-hierarchy walks during capture. Windows with a
         * Compose root always use verification regardless of this flag.
         */
        public var verifyScreenshotMaskAlignment: Boolean = false

        /**
         * How iOS captures each replay screenshot (iOS only). Defaults to [IosCaptureMethod.STANDARD]; set the
         * experimental [IosCaptureMethod.GPU] for less main-thread time on screens without Compose Multiplatform,
         * web views or Metal content.
         */
        public var iosCaptureMethod: IosCaptureMethod = IosCaptureMethod.STANDARD

        internal fun build(): SessionReplayConfig = SessionReplayConfig(this)
    }
}
