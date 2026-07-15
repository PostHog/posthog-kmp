package com.posthog.kmp

import java.io.File

/**
 * JVM (desktop) implementation backed by `com.posthog:posthog`, the pure-JVM core library
 * the Android SDK is built on.
 *
 * Android/iOS-only config options are ignored here:
 * [PostHogConfig.captureApplicationLifecycleEvents], [PostHogConfig.captureScreenViews],
 * [PostHogConfig.captureDeepLinks], [PostHogConfig.sessionRecording] and
 * [PostHogConfig.autocapture].
 */
@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val coreConfig = com.posthog.PostHogConfig(
        apiKey = config.apiKey,
        host = config.host
    ).apply {
        sdkName = SDK_NAME
        sdkVersion = PostHogKmpVersion.VERSION

        debug = config.debug
        sendFeatureFlagEvent = config.sendFeatureFlagEvent
        preloadFeatureFlags = config.preloadFeatureFlags

        flushAt = config.flushAt
        flushIntervalSeconds = config.flushIntervalSeconds
        maxQueueSize = config.maxQueueSize
        maxBatchSize = config.maxBatchSize

        optOut = config.optOut

        personProfiles = config.personProfiles.toCorePersonProfiles()
    }

    val home = System.getProperty("user.home")?.takeIf { it.isNotBlank() }
    val storageDir = File(home ?: System.getProperty("java.io.tmpdir") ?: ".", ".posthog-kmp")
    // the disk queue namespaces by API key itself; the preferences file doesn't
    coreConfig.storagePrefix = File(storageDir, "queue").absolutePath
    coreConfig.cachePreferences = PostHogFilePreferences(
        File(storageDir, "posthog-prefs-${coreConfig.apiKey}.json"),
        coreConfig
    )
    coreConfig.context = PostHogJvmContext(coreConfig)

    postHogInstance = com.posthog.PostHog.with(coreConfig)

    // the real logger is only installed during setup above; before that it is a no-op
    if (home == null) {
        coreConfig.logger.log(
            "user.home is not set; PostHog is storing its event queue and identity cache under " +
                "${storageDir.absolutePath}, which may not survive restarts."
        )
    }
}
