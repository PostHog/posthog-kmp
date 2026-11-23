package com.posthog.kmp

import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.replay.PostHogSessionReplayConfig

internal const val MISSING_APPLICATION_WARNING: String =
    "PostHog is disabled: PostHogContext() has no Application on Android. " +
        "Call PostHog.setup() with PostHogContext(application)."

/** Logs warnings that are shown even when `debug` is off. Replaced in tests. */
internal var androidWarningLogger: (String) -> Unit = { Log.w("PostHog", it) }

internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val application = context.application
    if (application == null) {
        androidWarningLogger(MISSING_APPLICATION_WARNING)
        return
    }
    postHogInstance = PostHogAndroid.with(application, config.toAndroidConfig())
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
        surveys = config.surveys
        sendFeatureFlagEvent = config.sendFeatureFlagEvent
        preloadFeatureFlags = config.preloadFeatureFlags

        flushAt = config.flushAt
        flushIntervalSeconds = config.flushIntervalSeconds
        maxQueueSize = config.maxQueueSize
        maxBatchSize = config.maxBatchSize

        optOut = config.optOut
        evaluationContexts = config.evaluationContexts

        personProfiles = config.personProfiles.toCorePersonProfiles()
        configureErrorTracking(config.errorTracking)
        configureCaptureAi(config)
        configureBeforeSend(config)

        config.sessionRecording?.let { sessionConfig ->
            sessionReplay = sessionConfig.enabled
            sessionReplayConfig = PostHogSessionReplayConfig(
                maskAllTextInputs = sessionConfig.maskAllTextInputs,
                maskAllImages = sessionConfig.maskAllImages,
                captureLogcat = sessionConfig.captureLogcat,
                throttleDelayMs = sessionConfig.throttleDelayMs
            ).apply {
                verifyScreenshotMaskAlignment = sessionConfig.verifyScreenshotMaskAlignment
            }
        }
    }
}
