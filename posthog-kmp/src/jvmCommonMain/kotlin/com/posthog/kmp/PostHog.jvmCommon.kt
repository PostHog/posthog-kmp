package com.posthog.kmp

import com.posthog.PostHogInterface
import java.util.Date

@Volatile
internal var postHogInstance: PostHogInterface? = null

internal const val SDK_NAME = "posthog-kmp"

internal actual fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?
) {
    postHogInstance?.capture(
        event = event,
        properties = properties,
        groups = groups,
        timestamp = timestamp?.let { Date(it) }
    )
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any>?) {
    postHogInstance?.screen(
        screenTitle = screenName,
        properties = properties
    )
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    postHogInstance?.identify(
        distinctId = distinctId,
        userProperties = userProperties,
        userPropertiesSetOnce = userPropertiesSetOnce
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

internal actual fun platformRegister(key: String, value: Any) {
    postHogInstance?.register(key, value)
}

internal actual fun platformUnregister(key: String) {
    postHogInstance?.unregister(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any>?
) {
    postHogInstance?.group(
        type = type,
        key = key,
        groupProperties = groupProperties
    )
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean {
    return postHogInstance?.isFeatureEnabled(key, defaultValue, sendFeatureFlagEvent) ?: defaultValue
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? {
    return postHogInstance?.getFeatureFlag(key, sendFeatureFlagEvent = sendFeatureFlagEvent)
}

internal actual fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult> {
    return postHogInstance?.getAllFeatureFlags()?.associate {
        it.key to it.toFeatureFlagResult()
    } ?: emptyMap()
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (callback != null) {
        postHogInstance?.reloadFeatureFlags { callback() }
    } else {
        postHogInstance?.reloadFeatureFlags()
    }
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? {
    return postHogInstance?.getFeatureFlagResult(key, sendFeatureFlagEvent)?.toFeatureFlagResult()
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any>?
) {
    postHogInstance?.captureException(
        throwable = throwable,
        properties = additionalProperties
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

internal fun PersonProfiles.toCorePersonProfiles(): com.posthog.PersonProfiles {
    return when (this) {
        PersonProfiles.ALWAYS -> com.posthog.PersonProfiles.ALWAYS
        PersonProfiles.IDENTIFIED_ONLY -> com.posthog.PersonProfiles.IDENTIFIED_ONLY
        PersonProfiles.NEVER -> com.posthog.PersonProfiles.NEVER
    }
}

internal fun com.posthog.FeatureFlagResult.toFeatureFlagResult(): FeatureFlagResult {
    return FeatureFlagResult(
        key = key,
        enabled = enabled,
        variant = variant,
        payload = payload
    )
}
