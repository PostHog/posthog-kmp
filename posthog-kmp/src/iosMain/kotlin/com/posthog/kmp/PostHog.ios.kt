@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import PostHogBridge.PostHogBridge
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * iOS implementation using the native PostHog iOS SDK via Swift bridge.
 *
 * This implementation provides full access to native PostHog features including:
 * - Session recording
 * - Autocapture
 * - Native networking and caching
 */


@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val sessionConfig = config.sessionRecording

    PostHogBridge.shared().setupWithApiKey(
        apiKey = config.apiKey,
        host = config.host,
        debug = config.debug,
        captureApplicationLifecycleEvents = config.captureApplicationLifecycleEvents,
        captureScreenViews = config.captureScreenViews,
        sendFeatureFlagEvent = config.sendFeatureFlagEvent,
        preloadFeatureFlags = config.preloadFeatureFlags,
        flushAt = config.flushAt.toLong(),
        flushIntervalSeconds = config.flushIntervalSeconds.toDouble(),
        maxQueueSize = config.maxQueueSize.toLong(),
        maxBatchSize = config.maxBatchSize.toLong(),
        optOut = config.optOut,
        personProfiles = config.personProfiles.name,
        sessionRecordingEnabled = sessionConfig?.enabled ?: false,
        sessionRecordingMaskAllTextInputs = sessionConfig?.maskAllTextInputs ?: true,
        sessionRecordingMaskAllImages = sessionConfig?.maskAllImages ?: false,
        sessionRecordingCaptureNetworkTelemetry = sessionConfig?.captureNetworkTelemetry ?: true,
        sessionRecordingCaptureLogs = sessionConfig?.captureLogs ?: true,
        sessionRecordingScreenshotMode = sessionConfig?.screenshot ?: false,
        autocapture = config.autocapture,
        sdkVersion = PostHogKmpVersion.VERSION
    )
}

internal actual fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().captureWithEvent(
        event,
        properties = properties as? Map<Any?, *>,
        groups = groups as? Map<Any?, *>,
        timestamp = timestamp?.let { NSDate.dateWithTimeIntervalSince1970(it.toDouble() / 1000.0) }
    )
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any>?) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().screenWithTitle(screenName, properties = properties as? Map<Any?, *>)
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().captureExceptionWithType(
        type = throwable::class.simpleName ?: "Exception",
        message = throwable.message,
        stackTrace = throwable.stackTraceToString(),
        properties = additionalProperties as? Map<Any?, *>
    )
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().identifyWithDistinctId(
        distinctId,
        userProperties = userProperties as? Map<Any?, *>,
        userPropertiesSetOnce = userPropertiesSetOnce as? Map<Any?, *>
    )
}

internal actual fun platformAlias(alias: String) {
    PostHogBridge.shared().aliasWithAlias(alias)
}

internal actual fun platformReset() {
    PostHogBridge.shared().reset()
}

internal actual fun platformGetDistinctId(): String? {
    return PostHogBridge.shared().getDistinctId()
}

internal actual fun platformRegister(key: String, value: Any) {
    PostHogBridge.shared().registerWithKey(key, value = value)
}

internal actual fun platformUnregister(key: String) {
    PostHogBridge.shared().unregisterWithKey(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().groupWithType(type, key = key, groupProperties = groupProperties as? Map<Any?, *>)
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean {
    // getFeatureFlag fires $feature_flag_called even for absent flags (matching Android) and lets us
    // honor defaultValue; the iOS SDK's isFeatureEnabled has no defaultValue parameter.
    val flagValue = PostHogBridge.shared().getFeatureFlag(key, sendFeatureFlagEvent = sendFeatureFlagEvent)
    return when (flagValue) {
        null -> defaultValue
        is String -> true
        else -> flagValue as? Boolean ?: false
    }
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? {
    return PostHogBridge.shared().getFeatureFlag(key, sendFeatureFlagEvent = sendFeatureFlagEvent)
}

internal actual fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult> {
    val results = PostHogBridge.shared().getAllFeatureFlags() ?: return emptyMap()
    val map = mutableMapOf<String, FeatureFlagResult>()
    for (entry in results) {
        val resultDict = entry as? Map<*, *> ?: continue
        val flagKey = resultDict["key"] as? String ?: continue
        map[flagKey] = FeatureFlagResult(
            key = flagKey,
            enabled = resultDict["enabled"] as? Boolean ?: false,
            variant = resultDict["variant"] as? String,
            payload = resultDict["payload"]
        )
    }
    return map
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (callback != null) {
        PostHogBridge.shared().reloadFeatureFlagsWithCallbackWithCallback {
            callback()
        }
    } else {
        PostHogBridge.shared().reloadFeatureFlags()
    }
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? {
    val resultDict = PostHogBridge.shared().getFeatureFlagResult(key, sendFeatureFlagEvent) ?: return null
    return FeatureFlagResult(
        key = resultDict["key"] as? String ?: key,
        enabled = resultDict["enabled"] as? Boolean ?: false,
        variant = resultDict["variant"] as? String,
        payload = resultDict["payload"]
    )
}

internal actual fun platformGetAnonymousId(): String? {
    return PostHogBridge.shared().getAnonymousId()
}

internal actual fun platformGetSessionId(): String? {
    return PostHogBridge.shared().getSessionId()
}

internal actual fun platformOptOut() {
    PostHogBridge.shared().optOut()
}

internal actual fun platformOptIn() {
    PostHogBridge.shared().optIn()
}

internal actual fun platformIsOptedOut(): Boolean {
    return PostHogBridge.shared().isOptedOut()
}

internal actual fun platformFlush() {
    PostHogBridge.shared().flush()
}

internal actual fun platformClose() {
    PostHogBridge.shared().close()
}

internal actual fun platformSetDebug(enabled: Boolean) {
    PostHogBridge.shared().setDebugWithEnabled(enabled)
}

internal actual fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    PostHogBridge.shared().setPersonPropertiesWithUserProperties(
        userProperties as? Map<Any?, *>,
        userPropertiesSetOnce = userPropertiesSetOnce as? Map<Any?, *>
    )
}
