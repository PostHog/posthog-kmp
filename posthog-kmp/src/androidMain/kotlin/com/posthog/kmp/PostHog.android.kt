package com.posthog.kmp

import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.replay.PostHogSessionReplayConfig
import java.util.Date

@Volatile
private var postHogInstance: PostHogInterface? = null

internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val androidConfig = PostHogAndroidConfig(
        apiKey = config.apiKey,
        host = config.host
    ).apply {
        debug = config.debug
        captureApplicationLifecycleEvents = config.captureApplicationLifecycleEvents
        captureScreenViews = config.captureScreenViews
        captureDeepLinks = config.captureDeepLinks
        sendFeatureFlagEvent = config.sendFeatureFlagEvent
        preloadFeatureFlags = config.preloadFeatureFlags

        flushAt = config.flushAt
        flushIntervalSeconds = config.flushIntervalSeconds
        maxQueueSize = config.maxQueueSize
        maxBatchSize = config.maxBatchSize

        optOut = config.optOut

        personProfiles = config.personProfiles.toAndroidPersonProfiles()

        // Session replay configuration
        config.sessionRecording?.let { sessionConfig ->
            sessionReplay = sessionConfig.enabled
            sessionReplayConfig = PostHogSessionReplayConfig(
                maskAllTextInputs = sessionConfig.maskAllTextInputs,
                maskAllImages = sessionConfig.maskAllImages,
                captureLogcat = sessionConfig.captureLogcat,
                screenshot = sessionConfig.screenshot,
                debouncerDelayMs = sessionConfig.debouncerDelayMs
            )
        }
    }

    postHogInstance = PostHogAndroid.with(context.application, androidConfig)
}

internal actual fun platformCapture(event: String, properties: Map<String, Any?>?, timestamp: Long?) {
    postHogInstance?.capture(
        event = event,
        properties = properties?.toPostHogProperties(),
        timestamp = timestamp?.let { Date(it) }
    )
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    postHogInstance?.screen(
        screenTitle = screenName,
        properties = properties?.toPostHogProperties()
    )
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
) {
    postHogInstance?.identify(
        distinctId = distinctId,
        userProperties = userProperties?.toPostHogProperties(),
        userPropertiesSetOnce = userPropertiesSetOnce?.toPostHogProperties()
    )
}

internal actual fun platformAlias(alias: String) {
    postHogInstance?.alias(alias)
}

internal actual fun platformReset() {
    postHogInstance?.reset()
}

internal actual fun platformGetDistinctId(): String? {
    return postHogInstance?.distinctId()
}

internal actual fun platformRegister(key: String, value: Any?) {
    value?.let { postHogInstance?.register(key, it) }
}

internal actual fun platformUnregister(key: String) {
    postHogInstance?.unregister(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    postHogInstance?.group(
        type = type,
        key = key,
        groupProperties = groupProperties?.toPostHogProperties()
    )
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean {
    return postHogInstance?.isFeatureEnabled(key, defaultValue, sendFeatureFlagEvent) ?: defaultValue
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? {
    return postHogInstance?.getFeatureFlag(key, sendFeatureFlagEvent)
}

internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    return postHogInstance?.getAllFeatureFlags()?.associate {
        val value: Any = if (it.value is String) {
            it.value as String
        } else if (it.value is Boolean) {
            it.value as Boolean
        } else true

        it.key to value
    } ?: emptyMap()
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (callback != null) {
        postHogInstance?.reloadFeatureFlags { callback() }
    } else {
        postHogInstance?.reloadFeatureFlags()
    }
}

internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    // Not available in PostHog Android SDK
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? {
    return postHogInstance?.getFeatureFlagResult(key, sendFeatureFlagEvent)?.toFeatureFlagResult()
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any?>?
) {
    postHogInstance?.captureException(
        throwable = throwable,
        properties = additionalProperties?.toPostHogProperties()
    )
}

internal actual fun platformGetAnonymousId(): String? {
    return postHogInstance?.getAnonymousId()
}

internal actual fun platformGetSessionId(): String? {
    return postHogInstance?.getSessionId()?.toString()
}

internal actual fun platformOptOut() {
    postHogInstance?.optOut()
}

internal actual fun platformOptIn() {
    postHogInstance?.optIn()
}

internal actual fun platformIsOptedOut(): Boolean {
    return postHogInstance?.isOptOut() ?: false
}

internal actual fun platformFlush() {
    postHogInstance?.flush()
}

internal actual fun platformClose() {
    postHogInstance?.close()
    postHogInstance = null
}

internal actual fun platformSetDebug(enabled: Boolean) {
    postHogInstance?.debug(enabled)
}

internal actual fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    postHogInstance?.setPersonProperties(userProperties, userPropertiesSetOnce)
}

// Helper to convert nullable values to non-null map expected by PostHog
private fun Map<String, Any?>.toPostHogProperties(): Map<String, Any> {
    return this.filterValues { it != null }.mapValues { it.value!! }
}

private fun PersonProfiles.toAndroidPersonProfiles(): com.posthog.PersonProfiles{
    return when (this) {
        PersonProfiles.ALWAYS -> com.posthog.PersonProfiles.ALWAYS
        PersonProfiles.IDENTIFIED_ONLY -> com.posthog.PersonProfiles.IDENTIFIED_ONLY
        PersonProfiles.NEVER -> com.posthog.PersonProfiles.NEVER
    }
}

private fun com.posthog.FeatureFlagResult.toFeatureFlagResult(): FeatureFlagResult {
    return FeatureFlagResult(
        key = key,
        enabled = if (value is Boolean) value as Boolean else true,
        variant = if (value is String) value as String else null,
        payload = payload
    )
}
