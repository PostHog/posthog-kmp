@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.dateWithTimeIntervalSince1970
import swiftPMImport.com.posthog.posthog.kmp.BoxedBeforeSendBlock
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogEvent as NativePostHogEvent
import swiftPMImport.com.posthog.posthog.kmp.PostHogFeatureFlagResult as NativeFeatureFlagResult
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesAlways
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesIdentifiedOnly
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesNever
import swiftPMImport.com.posthog.posthog.kmp.PostHogSDK

/**
 * iOS implementation using the native PostHog iOS SDK imported through SwiftPM.
 *
 * This implementation provides full access to native PostHog features including:
 * - Session recording
 * - Autocapture
 * - Native networking and caching
 */


@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val sessionConfig = config.sessionRecording
    val nativeConfig = NativePostHogConfig(apiKey = config.apiKey, host = config.host).apply {
        debug = config.debug
        captureApplicationLifecycleEvents = config.captureApplicationLifecycleEvents
        captureScreenViews = config.captureScreenViews
        sendFeatureFlagEvent = config.sendFeatureFlagEvent
        preloadFeatureFlags = config.preloadFeatureFlags
        flushAt = config.flushAt.toLong()
        flushIntervalSeconds = config.flushIntervalSeconds.toDouble()
        maxQueueSize = config.maxQueueSize.toLong()
        maxBatchSize = config.maxBatchSize.toLong()
        optOut = config.optOut
        personProfiles = when (config.personProfiles) {
            PersonProfiles.ALWAYS -> PostHogPersonProfilesAlways
            PersonProfiles.NEVER -> PostHogPersonProfilesNever
            PersonProfiles.IDENTIFIED_ONLY -> PostHogPersonProfilesIdentifiedOnly
        }
        setDefaultPersonProperties = true
        captureElementInteractions = config.autocapture

        if (sessionConfig?.enabled == true) {
            sessionReplay = true
            sessionReplayConfig.maskAllTextInputs = sessionConfig.maskAllTextInputs
            sessionReplayConfig.maskAllImages = sessionConfig.maskAllImages
            sessionReplayConfig.captureNetworkTelemetry = sessionConfig.captureNetworkTelemetry
            sessionReplayConfig.captureLogs = sessionConfig.captureLogs
            sessionReplayConfig.screenshotMode = sessionConfig.screenshot
        }

        config.errorTracking?.let { errorTracking ->
            errorTrackingConfig.autoCapture = errorTracking.autoCapture
            errorTrackingConfig.inAppIncludes = errorTrackingConfig.inAppIncludes + errorTracking.inAppIncludes
            errorTrackingConfig.ignoredExceptionTypes = errorTrackingConfig.ignoredExceptionTypes +
                errorTracking.ignoredExceptionTypes
                    .flatMap { listOfNotNull(it.simpleName, it.qualifiedName) }
                    .distinct()
            errorTrackingConfig.inAppExcludes = errorTrackingConfig.inAppExcludes + errorTracking.inAppExcludes
            errorTrackingConfig.inAppByDefault = errorTracking.inAppByDefault
        }

        // The native SDK's top-level metadata globals are not exposed through Objective-C.
        // Enrich every event here before running the user-provided KMP callbacks instead.
        setBeforeSend(listOf(BoxedBeforeSendBlock { event -> processBeforeSend(config, event) }))
    }

    PostHogSDK.shared.setup(nativeConfig)
    configureUnhandledKotlinExceptionCapture(
        enabled = config.errorTracking?.autoCapture == true,
        debug = config.debug
    )
}

private fun processBeforeSend(config: PostHogConfig, event: NativePostHogEvent?): NativePostHogEvent? {
    event ?: return null
    val properties = event.properties
        .filterKeys { it is String }
        .mapKeys { it.key as String }
        .filterValues { it != null }
        .toMutableMap()
        .apply {
            this["\$lib"] = "posthog-kmp"
            this["\$lib_version"] = PostHogKmpVersion.VERSION
        }

    val processed = config.runBeforeSend(
        PostHogEvent(event.event, event.distinctId, properties)
    ) {
        if (config.debug) NSLog("[PostHog] Before-send callback failed; event was dropped.")
    } ?: return null

    event.event = processed.event
    event.distinctId = processed.distinctId
    event.properties = processed.properties.filterValues { it != null }
    return event
}

internal actual fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.captureWithEvent(
        event = event,
        distinctId = null,
        properties = properties as? Map<Any?, *>,
        userProperties = null,
        userPropertiesSetOnce = null,
        groups = groups as? Map<Any?, *>,
        timestamp = timestamp?.toNSDate()
    )
}

internal fun Long.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(toDouble() / 1000.0)

internal actual fun platformScreen(screenName: String, properties: Map<String, Any>?) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.screenWithTitle(screenName, properties = properties as? Map<Any?, *>)
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.captureExceptionWithNSException(
        exception = throwable.toNSException(),
        properties = additionalProperties as? Map<Any?, *>
    )
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.identifyWithDistinctId(
        distinctId,
        userProperties = userProperties as? Map<Any?, *>,
        userPropertiesSetOnce = userPropertiesSetOnce as? Map<Any?, *>
    )
}

internal actual fun platformAlias(alias: String) {
    PostHogSDK.shared.alias(alias)
}

internal actual fun platformReset() {
    PostHogSDK.shared.reset()
}

internal actual fun platformGetDistinctId(): String? = PostHogSDK.shared.getDistinctId()

internal actual fun platformRegister(key: String, value: Any) {
    PostHogSDK.shared.registerProperties(mapOf(key to value))
}

internal actual fun platformUnregister(key: String) {
    PostHogSDK.shared.unregisterProperties(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.groupWithType(type, key, groupProperties as? Map<Any?, *>)
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean {
    // getFeatureFlag fires $feature_flag_called even for absent flags (matching Android) and lets us
    // honor defaultValue; the iOS SDK's isFeatureEnabled has no defaultValue parameter.
    val flagValue = PostHogSDK.shared.getFeatureFlagWithKey(key, sendFeatureFlagEvent)
    return when (flagValue) {
        null -> defaultValue
        is String -> true
        else -> flagValue as? Boolean ?: false
    }
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? =
    PostHogSDK.shared.getFeatureFlagWithKey(key, sendFeatureFlagEvent)

internal actual fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult> =
    PostHogSDK.shared.getAllFeatureFlags()
        ?.filterIsInstance<NativeFeatureFlagResult>()
        ?.associate { result -> result.key to result.toFeatureFlagResult() }
        ?: emptyMap()

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (callback != null) {
        PostHogSDK.shared.reloadFeatureFlagsWithCallback(callback)
    } else {
        PostHogSDK.shared.reloadFeatureFlags()
    }
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? =
    PostHogSDK.shared.getFeatureFlagResultWithKey(key, sendFeatureFlagEvent)?.toFeatureFlagResult()

private fun NativeFeatureFlagResult.toFeatureFlagResult(): FeatureFlagResult = FeatureFlagResult(
    key = key,
    enabled = enabled,
    variant = variant,
    payload = payload
)

internal actual fun platformGetAnonymousId(): String? = PostHogSDK.shared.getAnonymousId()

internal actual fun platformGetSessionId(): String? = PostHogSDK.shared.getSessionId()

internal actual fun platformOptOut() {
    PostHogSDK.shared.optOut()
}

internal actual fun platformOptIn() {
    PostHogSDK.shared.optIn()
}

internal actual fun platformIsOptedOut(): Boolean = PostHogSDK.shared.isOptOut()

internal actual fun platformFlush() {
    PostHogSDK.shared.flush()
}

internal actual fun platformClose() {
    configureUnhandledKotlinExceptionCapture(false)
    PostHogSDK.shared.close()
}

internal actual fun platformSetDebug(enabled: Boolean) {
    PostHogSDK.shared.debug(enabled)
}

internal actual fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.setPersonPropertiesWithUserPropertiesToSet(
        userProperties as? Map<Any?, *>,
        userPropertiesToSetOnce = userPropertiesSetOnce as? Map<Any?, *>
    )
}
