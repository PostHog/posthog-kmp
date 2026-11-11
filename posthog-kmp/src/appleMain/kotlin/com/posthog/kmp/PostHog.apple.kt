@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSLog
import platform.Foundation.NSMutableDictionary
import platform.Foundation.addEntriesFromDictionary
import platform.Foundation.dateWithTimeIntervalSince1970
import swiftPMImport.com.posthog.posthog.kmp.BoxedBeforeSendBlock
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogEvent as NativePostHogEvent
import swiftPMImport.com.posthog.posthog.kmp.PostHogFeatureFlagResult as NativeFeatureFlagResult
import swiftPMImport.com.posthog.posthog.kmp.PostHogFeatureFlagsLoaded as NativeFeatureFlagsLoaded
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesAlways
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesIdentifiedOnly
import swiftPMImport.com.posthog.posthog.kmp.PostHogPersonProfilesNever
import swiftPMImport.com.posthog.posthog.kmp.PostHogSDK
import swiftPMImport.com.posthog.posthog.kmp.setSdkName

/**
 * Apple implementation using posthog-ios imported through SwiftPM.
 * Networking and storage are delegated to the native SDK; replay and UIKit
 * interaction autocapture are configured only on iOS.
 */


@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    // posthog-ios stamps `$lib`/`$lib_version` and the PostHog-Sdk-Info header from this name and version.
    PostHogSDK.setSdkName("posthog-kmp", version = PostHogKmpVersion.VERSION)
    val nativeConfig = NativePostHogConfig(projectToken = config.projectToken, host = config.host).apply {
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
        evaluationContexts = config.evaluationContexts
        personProfiles = when (config.personProfiles) {
            PersonProfiles.ALWAYS -> PostHogPersonProfilesAlways
            PersonProfiles.NEVER -> PostHogPersonProfilesNever
            PersonProfiles.IDENTIFIED_ONLY -> PostHogPersonProfilesIdentifiedOnly
        }
        setDefaultPersonProperties = true
        configurePlatformCapture(config)

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

        if (config.beforeSend.isNotEmpty()) {
            setBeforeSend(listOf(BoxedBeforeSendBlock { event -> processBeforeSend(config, event) }))
        }
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

    val options = event.options
        .filterKeys { it is String }
        .mapKeys { it.key as String }
        .toEventOptions()

    val processed = config.runBeforeSend(
        PostHogEvent(event.event, event.distinctId, properties.toMap(), options)
    ) {
        if (config.debug) NSLog("[PostHog] Before-send callback failed; event was dropped.")
    } ?: return null

    event.event = processed.event
    event.distinctId = processed.distinctId
    // Only what the callback actually replaced is rebuilt. Carrying the rest over as the native
    // objects they already are keeps a $snapshot payload off the main thread's critical path.
    val (removedKeys, changedValues) = propertyDelta(properties, processed.properties)
    event.properties = event.properties.withChanges(removedKeys, changedValues)
    // An event the callback built without options keeps the ones it was given
    val (removedOptions, changedOptions) = eventOptionChanges(options, processed.options ?: options)
    if (removedOptions.isNotEmpty() || changedOptions.isNotEmpty()) {
        event.options = event.options.withChanges(removedOptions, changedOptions)
    }
    return event
}

/**
 * The keys a before-send callback removed, and the entries it added or replaced. Values are compared
 * by identity: the map handed to the callback holds each value once, so anything it did not replace
 * comes back as the same reference.
 *
 * Setting a property to null removes it, as on Android and the JVM. Properties the native SDK set to
 * `NSNull` are not replaced by that, since the callback hands them straight back.
 */
internal fun propertyDelta(
    before: Map<String, Any?>,
    after: Map<String, Any?>
): Pair<Set<String>, Map<String, Any?>> {
    // An added key reads back as null from [before], so absence is tested apart from the value.
    val changed = after.filter { (key, value) -> key !in before || before[key] !== value }
    val nulled = changed.filterValues { it == null }.keys
    return ((before.keys - after.keys) + nulled) to (changed - nulled)
}

/**
 * Applies what a before-send callback removed or replaced to [this], without reading the values
 * it already holds into Kotlin. Reading them back would re-box every boolean the native SDK set.
 */
internal fun Map<Any?, *>.withChanges(
    removedKeys: Set<String>,
    changedValues: Map<String, Any?>
): Map<Any?, *> {
    val updated = NSMutableDictionary()
    updated.addEntriesFromDictionary(this)
    removedKeys.forEach { updated.removeObjectForKey(it) }
    if (changedValues.isNotEmpty()) {
        updated.addEntriesFromDictionary(changedValues.toNativeProperties())
    }
    @Suppress("UNCHECKED_CAST")
    return updated.copy() as Map<Any?, *>
}

internal actual fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?,
    options: PostHogEventOptions?
) {
    @Suppress("UNCHECKED_CAST")
    PostHogSDK.shared.captureWithEvent(
        event = event,
        distinctId = null,
        properties = properties?.toNativeProperties(),
        userProperties = null,
        userPropertiesSetOnce = null,
        // Group values are strings; only booleans need rebuilding on the Objective-C side.
        groups = groups as? Map<Any?, *>,
        timestamp = timestamp?.toNSDate(),
        options = options.toNativeOptions()
    )
}

internal actual fun platformCaptureAi(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?,
    options: PostHogEventOptions?
): String? {
    @Suppress("UNCHECKED_CAST")
    return PostHogSDK.shared.captureAiWithEvent(
        event = event,
        distinctId = null,
        properties = properties?.toNativeProperties(),
        userProperties = null,
        userPropertiesSetOnce = null,
        groups = groups as? Map<Any?, *>,
        timestamp = timestamp?.toNSDate(),
        options = options.toNativeOptions()
    )
}

private fun PostHogEventOptions?.toNativeOptions(): Map<Any?, *>? = this?.toOptionsMap()?.toNativeProperties()

internal fun Long.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(toDouble() / 1000.0)

internal actual fun platformScreen(screenName: String, properties: Map<String, Any>?, options: PostHogEventOptions?) {
    PostHogSDK.shared.screenWithTitle(
        screenName,
        properties = properties?.toNativeProperties(),
        options = options.toNativeOptions()
    )
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any>?,
    options: PostHogEventOptions?
) {
    PostHogSDK.shared.captureExceptionWithNSException(
        exception = throwable.toNSException(),
        properties = additionalProperties?.toNativeProperties(),
        options = options.toNativeOptions()
    )
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?,
    options: PostHogEventOptions?
) {
    PostHogSDK.shared.identifyWithDistinctId(
        distinctId,
        userProperties = userProperties?.toNativeProperties(),
        userPropertiesSetOnce = userPropertiesSetOnce?.toNativeProperties(),
        options = options.toNativeOptions()
    )
}

internal actual fun platformAlias(alias: String, options: PostHogEventOptions?) {
    PostHogSDK.shared.alias(alias, options = options.toNativeOptions())
}

internal actual fun platformReset() {
    PostHogSDK.shared.reset()
}

internal actual fun platformGetDistinctId(): String? = PostHogSDK.shared.getDistinctId()

internal actual fun platformRegister(key: String, value: Any) {
    // Super properties are persisted as JSON, so a re-boxed boolean stays a number for good.
    PostHogSDK.shared.registerProperties(mapOf(key to value).toNativeProperties())
}

internal actual fun platformUnregister(key: String) {
    PostHogSDK.shared.unregisterProperties(key)
}

internal actual fun platformRegisterOptions(options: Map<String, Any>) {
    PostHogSDK.shared.registerOptionsForWrapper(options.toNativeProperties())
}

internal actual fun platformUnregisterOptions(key: String) {
    PostHogSDK.shared.unregisterOptionsForWrapper(key)
}

internal actual fun platformSetAppLanguage(language: String?) {
    PostHogSDK.shared.setAppLanguage(language)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any>?,
    options: PostHogEventOptions?
) {
    PostHogSDK.shared.groupWithType(type, key, groupProperties?.toNativeProperties(), options = options.toNativeOptions())
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean =
    PostHogSDK.shared.isFeatureEnabledWithKey(key, defaultValue = defaultValue, sendFeatureFlagEvent = sendFeatureFlagEvent)

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? =
    PostHogSDK.shared.getFeatureFlagWithKey(key, sendFeatureFlagEvent)

internal actual fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult> =
    PostHogSDK.shared.getAllFeatureFlags()
        ?.filterIsInstance<NativeFeatureFlagResult>()
        ?.associate { result -> result.key to result.toFeatureFlagResult() }
        ?: emptyMap()

internal actual fun platformReloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)?) {
    if (callback != null) {
        PostHogSDK.shared.reloadFeatureFlagsWithCallback { loaded -> deliverFeatureFlags(callback, loaded) }
    } else {
        PostHogSDK.shared.reloadFeatureFlags()
    }
}

internal actual fun platformOnFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription {
    val subscription = PostHogSDK.shared.onFeatureFlags { loaded -> deliverFeatureFlags(callback, loaded) }
    return PostHogFeatureFlagsSubscription { subscription.unsubscribe() }
}

/** A Kotlin exception thrown back into the native SDK would terminate the app, so it stops here. */
private fun deliverFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit, loaded: NativeFeatureFlagsLoaded?) {
    val featureFlags = loaded?.variants
        ?.filterKeys { it is String }
        ?.mapKeys { it.key as String }
        .orEmpty()
    try {
        callback(PostHogFeatureFlagsLoaded(featureFlags, errorsLoading = loaded?.errorsLoading ?: true))
    } catch (e: Throwable) {
        if (currentConfig?.debug == true) NSLog("[PostHog] Feature flags callback failed: ${e.message}")
    }
}

internal actual fun platformSetPersonPropertiesForFlags(properties: Map<String, Any>, reloadFeatureFlags: Boolean) {
    PostHogSDK.shared.setPersonPropertiesForFlagsWithProperties(
        properties.toNativeProperties(),
        reloadFeatureFlags = reloadFeatureFlags
    )
}

internal actual fun platformResetPersonPropertiesForFlags(reloadFeatureFlags: Boolean) {
    PostHogSDK.shared.resetPersonPropertiesForFlagsWithReloadFeatureFlags(reloadFeatureFlags)
}

internal actual fun platformSetGroupPropertiesForFlags(
    type: String,
    properties: Map<String, Any>,
    reloadFeatureFlags: Boolean
) {
    PostHogSDK.shared.setGroupPropertiesForFlags(
        type,
        properties = properties.toNativeProperties(),
        reloadFeatureFlags = reloadFeatureFlags
    )
}

internal actual fun platformResetGroupPropertiesForFlags(type: String?, reloadFeatureFlags: Boolean) {
    if (type == null) {
        PostHogSDK.shared.resetGroupPropertiesForFlagsWithReloadFeatureFlags(reloadFeatureFlags)
    } else {
        PostHogSDK.shared.resetGroupPropertiesForFlagsWithGroupType(type, reloadFeatureFlags = reloadFeatureFlags)
    }
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? =
    PostHogSDK.shared.getFeatureFlagResultWithKey(key, sendFeatureFlagEvent)?.toFeatureFlagResult()

private fun NativeFeatureFlagResult.toFeatureFlagResult(): FeatureFlagResult = featureFlagResult(
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
    userPropertiesSetOnce: Map<String, Any>?,
    options: PostHogEventOptions?
) {
    PostHogSDK.shared.setPersonPropertiesWithUserPropertiesToSet(
        userProperties?.toNativeProperties(),
        userPropertiesToSetOnce = userPropertiesSetOnce?.toNativeProperties(),
        options = options.toNativeOptions()
    )
}
