package com.posthog.kmp

import com.posthog.PostHogFeatureFlagsCallback
import com.posthog.PostHogInterface
import java.util.Date

@Volatile
internal var postHogInstance: PostHogInterface? = null
    set(value) {
        field = value
        featureFlagsListeners.attachAll(value)
    }

/**
 * [PostHog.onFeatureFlags] registrations. Each one is registered on the native instance, so it gets
 * the native replay and threading, and is moved to the new instance on every setup.
 */
private val featureFlagsListeners = FeatureFlagsListeners()

private class FeatureFlagsListeners {
    private class Registration(val callback: PostHogFeatureFlagsCallback) {
        var native: com.posthog.PostHogFeatureFlagsSubscription? = null
    }

    private val registrations = mutableListOf<Registration>()

    @Synchronized
    fun add(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription {
        val registration = Registration { loaded -> callback(loaded.toFeatureFlagsLoaded()) }
        registrations.add(registration)
        registration.native = postHogInstance?.onFeatureFlags(registration.callback)
        return PostHogFeatureFlagsSubscription { remove(registration) }
    }

    @Synchronized
    fun attachAll(instance: PostHogInterface?) {
        registrations.forEach { registration ->
            registration.native?.unsubscribe()
            registration.native = instance?.onFeatureFlags(registration.callback)
        }
    }

    @Synchronized
    private fun remove(registration: Registration) {
        registrations.remove(registration)
        registration.native?.unsubscribe()
        registration.native = null
    }
}

private fun com.posthog.PostHogFeatureFlagsLoaded.toFeatureFlagsLoaded(): PostHogFeatureFlagsLoaded =
    PostHogFeatureFlagsLoaded(variants, errorsLoading)

internal const val SDK_NAME = "posthog-kmp"

internal fun com.posthog.PostHogConfig.configureErrorTracking(config: ErrorTrackingConfig?) {
    config ?: return
    errorTrackingConfig.autoCapture = config.autoCapture
    errorTrackingConfig.inAppIncludes.addAll(config.inAppIncludes)
    errorTrackingConfig.ignoredExceptionTypes.addAll(config.ignoredExceptionTypes.map { it.java })
}

internal fun com.posthog.PostHogConfig.configureBeforeSend(config: PostHogConfig) {
    if (config.beforeSend.isEmpty()) return

    addBeforeSend { event ->
        config.runBeforeSend(
            PostHogEvent(
                event = event.event,
                distinctId = event.distinctId,
                properties = event.properties.orEmpty()
            ),
            onError = {
                if (config.debug) logger.log("Before-send callback failed; event was dropped.")
            }
        )?.let { processed ->
            event.copy(
                event = processed.event,
                distinctId = processed.distinctId,
                properties = processed.properties.mapNotNull { (key, value) ->
                    value?.let { key to it }
                }.toMap().toMutableMap()
            )
        }
    }
}

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

internal actual fun platformReloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)?) {
    val instance = postHogInstance
    when {
        callback == null -> instance?.reloadFeatureFlags()
        instance == null -> callback(PostHogFeatureFlagsLoaded(emptyMap(), errorsLoading = true))
        else -> instance.reloadFeatureFlags { loaded -> callback(loaded.toFeatureFlagsLoaded()) }
    }
}

internal actual fun platformOnFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription =
    featureFlagsListeners.add(callback)

internal actual fun platformSetPersonPropertiesForFlags(properties: Map<String, Any>, reloadFeatureFlags: Boolean) {
    postHogInstance?.setPersonPropertiesForFlags(properties, reloadFeatureFlags)
}

internal actual fun platformResetPersonPropertiesForFlags(reloadFeatureFlags: Boolean) {
    postHogInstance?.resetPersonPropertiesForFlags(reloadFeatureFlags)
}

internal actual fun platformSetGroupPropertiesForFlags(
    type: String,
    properties: Map<String, Any>,
    reloadFeatureFlags: Boolean
) {
    postHogInstance?.setGroupPropertiesForFlags(type, properties, reloadFeatureFlags)
}

internal actual fun platformResetGroupPropertiesForFlags(type: String?, reloadFeatureFlags: Boolean) {
    postHogInstance?.resetGroupPropertiesForFlags(type, reloadFeatureFlags)
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
    return featureFlagResult(
        key = key,
        enabled = enabled,
        variant = variant,
        payload = payload
    )
}
