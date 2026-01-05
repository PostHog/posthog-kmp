@file:OptIn(ExperimentalForeignApi::class)

package com.samuolis.posthog

import PostHogBridge.PostHogBridge
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation using the native PostHog iOS SDK via Swift bridge.
 *
 * This implementation provides full access to native PostHog features including:
 * - Session recording
 * - Surveys
 * - Autocapture
 * - Native networking and caching
 */

private var currentConfig: PostHogConfig? = null

@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    currentConfig = config

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
        sessionRecordingEnabled = config.sessionRecording?.enabled ?: false,
        autocapture = config.autocapture,
        environment = "production"
    )
}

internal actual fun platformCapture(event: String, properties: Map<String, Any?>?) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().captureWithEvent(event, properties = properties as? Map<Any?, *>)
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().screenWithTitle(screenName, properties = properties as? Map<Any?, *>)
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
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

internal actual fun platformRegister(key: String, value: Any?) {
    value?.let {
        PostHogBridge.shared().registerWithKey(key, value = it)
    }
}

internal actual fun platformUnregister(key: String) {
    PostHogBridge.shared().unregisterWithKey(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogBridge.shared().groupWithType(type, key = key, groupProperties = groupProperties as? Map<Any?, *>)
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean): Boolean {
    return PostHogBridge.shared().isFeatureEnabled(key)
}

internal actual fun platformGetFeatureFlag(key: String): Any? {
    return PostHogBridge.shared().getFeatureFlag(key)
}

internal actual fun platformGetFeatureFlagPayload(key: String): Any? {
    return PostHogBridge.shared().getFeatureFlagPayload(key)
}

internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    // Not available in PostHog iOS SDK - return empty map
    return emptyMap()
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

internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    // Not available in PostHog iOS SDK
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
    currentConfig = null
}

internal actual fun platformSetDebug(enabled: Boolean) {
    PostHogBridge.shared().setDebugWithEnabled(enabled)
}
