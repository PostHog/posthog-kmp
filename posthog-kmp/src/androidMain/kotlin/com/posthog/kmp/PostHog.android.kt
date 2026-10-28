package com.posthog.kmp

import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.replay.PostHogSessionReplayConfig

internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    postHogInstance = PostHogAndroid.with(context.application, config.toAndroidConfig())
}

internal fun PostHogConfig.toAndroidConfig(): PostHogAndroidConfig {
    val config = this
    return PostHogAndroidConfig(config.projectToken, config.host).apply {
        sdkName = SDK_NAME
        sdkVersion = PostHogKmpVersion.VERSION

        debug = config.debug
        captureApplicationLifecycleEvents = config.captureApplicationLifecycleEvents
        captureScreenViews = config.captureScreenViews
        captureDeepLinks = config.captureDeepLinks
        capturePushNotificationSubscriptions = config.capturePushNotificationSubscriptions
        capturePushNotificationOpened = config.capturePushNotificationOpened
        sendFeatureFlagEvent = config.sendFeatureFlagEvent
        preloadFeatureFlags = config.preloadFeatureFlags

        flushAt = config.flushAt
        flushIntervalSeconds = config.flushIntervalSeconds
        maxQueueSize = config.maxQueueSize
        maxBatchSize = config.maxBatchSize

        optOut = config.optOut

        personProfiles = config.personProfiles.toCorePersonProfiles()
        configureErrorTracking(config.errorTracking)
        configureBeforeSend(config)

        config.sessionRecording?.let { sessionConfig ->
            sessionReplay = sessionConfig.enabled
            sessionReplayConfig = PostHogSessionReplayConfig(
                maskAllTextInputs = sessionConfig.maskAllTextInputs,
                maskAllImages = sessionConfig.maskAllImages,
                captureLogcat = sessionConfig.captureLogcat,
                screenshot = sessionConfig.screenshot,
                throttleDelayMs = sessionConfig.debouncerDelayMs
            ).apply {
                verifyScreenshotMaskAlignment = sessionConfig.verifyScreenshotMaskAlignment
            }
        }
    }
}
