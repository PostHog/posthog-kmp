@file:OptIn(ExperimentalJsExport::class)

package com.posthog.kmp

/**
 * JavaScript implementation using the official posthog-js library.
 *
 * This implementation wraps the posthog-js npm package for full
 * browser compatibility including session recording, autocapture,
 * and all web-specific features.
 */

// External declarations for posthog-js
@JsModule("posthog-js")
@JsNonModule
private external object PostHogJsModule

private val PostHogJs: dynamic
    get() {
        val raw: dynamic = PostHogJsModule
        return when {
            raw?.init != null -> raw
            raw?.default?.init != null -> raw.default
            raw?.posthog?.init != null -> raw.posthog
            else -> raw
        }
    }


@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val options = js("{}")
    options["api_host"] = config.host
    options["debug"] = config.debug
    options["capture_pageview"] = config.captureScreenViews
    options["capture_pageleave"] = config.captureScreenViews
    options["autocapture"] = config.autocapture
    options["persistence"] = "localStorage"
    options["defaults"] = "2026-05-30"
    options["bootstrap"] = js("{}")
    
    options["person_profiles"] = when (config.personProfiles) {
        PersonProfiles.ALWAYS -> "always"
        PersonProfiles.IDENTIFIED_ONLY -> "identified_only"
        PersonProfiles.NEVER -> "never"
    }
    
    // Note: sendFeatureFlagEvent has no direct per-call posthog-js equivalent but
    // "capture_performance" and per-call options exist

    // Session recording config
    config.sessionRecording?.let { sessionConfig ->
        if (sessionConfig.enabled) {
            options["session_recording"] = js("{}")
            options["session_recording"]["maskAllInputs"] = sessionConfig.maskAllTextInputs
            options["session_recording"]["maskAllImages"] = sessionConfig.maskAllImages

            options["session_recording"]["networkCaptureConfig"] = js("{}")
            options["session_recording"]["networkCaptureConfig"]["recordHeaders"] = sessionConfig.captureNetworkTelemetry
            options["session_recording"]["captureLogs"] = sessionConfig.captureLogs
        }
    }

    // Feature flags config
    options["advanced_disable_feature_flags"] = !config.preloadFeatureFlags

    PostHogJs.init(config.apiKey, options)
    if (config.optOut) {
        PostHogJs.opt_out_capturing()
    }
}

internal actual fun platformCapture(event: String, properties: Map<String, Any?>?, timestamp: Long?) {
    val options = js("{}")
    if (timestamp != null) {
        options["timestamp"] = kotlin.js.Date(timestamp.toDouble())
    }

    if (properties != null) {
        PostHogJs.capture(event, properties.toJsObject(), options)
    } else {
        PostHogJs.capture(event, null, options)
    }
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    val screenProperties = buildMap {
        put("\$screen_name", screenName)
        properties?.forEach { (key, value) -> put(key, value) }
    }
    PostHogJs.capture("\$screen", screenProperties.toJsObject())
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
) {
    PostHogJs.identify(
        distinctId,
        userProperties?.toJsObject(),
        userPropertiesSetOnce?.toJsObject()
    )
}

internal actual fun platformAlias(alias: String) {
    PostHogJs.alias(alias)
}

internal actual fun platformReset() {
    PostHogJs.reset(true)
}

internal actual fun platformGetDistinctId(): String? {
    return PostHogJs.get_distinct_id()
}

internal actual fun platformRegister(key: String, value: Any?) {
    val props = js("{}")
    props[key] = value
    PostHogJs.register(props)
}

internal actual fun platformUnregister(key: String) {
    PostHogJs.unregister(key)
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    if (groupProperties != null) {
        PostHogJs.group(type, key, groupProperties.toJsObject())
    } else {
        PostHogJs.group(type, key)
    }
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean, sendFeatureFlagEvent: Boolean): Boolean {
    val options = js("{}")
    options["send_event"] = sendFeatureFlagEvent
    return PostHogJs.isFeatureEnabled(key, options) ?: defaultValue
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? {
    val options = js("{}")
    options["send_event"] = sendFeatureFlagEvent
    return PostHogJs.getFeatureFlag(key, options)
}

internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    return try {
        val flagsDynamic = PostHogJs.featureFlags.getFlagsWithDetails()
        if (flagsDynamic == null || flagsDynamic == undefined) {
            return emptyMap()
        }
        val keys = js("Object.keys")(flagsDynamic) as Array<String>
        val map = mutableMapOf<String, Any?>()
        for (key in keys) {
            val detail = flagsDynamic[key]
            if (detail != null && detail != undefined) {
                val isEnabled = detail.enabled as? Boolean ?: false
                val variant = detail.variant as? String
                val payload = detail.payload
                
                map[key] = FeatureFlagResult(
                    key = key,
                    enabled = isEnabled,
                    variant = variant,
                    payload = payload
                )
            }
        }
        map
    } catch (e: Throwable) {
        emptyMap()
    }
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (callback != null) {
        var unSub: dynamic = null
        var invokeUnSub = false
        unSub = PostHogJs.onFeatureFlags {
            callback()
            if (unSub != null) {
                unSub()
            } else {
                invokeUnSub = true
            }
        }
        if (unSub != null && invokeUnSub) {
            unSub()
        }
    }
    PostHogJs.reloadFeatureFlags()
}

internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    PostHogJs.featureFlags.overrideFeatureFlags(flags.toJsObject())
}

internal actual fun platformGetFeatureFlagResult(key: String, sendFeatureFlagEvent: Boolean): FeatureFlagResult? {
    val options = js("{}")
    options["send_event"] = sendFeatureFlagEvent
    val result = PostHogJs.getFeatureFlagResult(key, options)
    if (result == null || result == undefined) {
        return null
    }

    val isEnabled = result.enabled as? Boolean ?: false
    val variant = result.variant as? String
    val payload = result.payload

    return FeatureFlagResult(
        key = key,
        enabled = isEnabled,
        variant = variant,
        payload = payload
    )
}

internal actual fun platformCaptureException(
    throwable: Throwable,
    additionalProperties: Map<String, Any?>?
) {
    PostHogJs.captureException(throwable, additionalProperties?.toJsObject())
}

internal actual fun platformGetAnonymousId(): String? {
    return PostHogJs.get_property("\$device_id")
}

internal actual fun platformGetSessionId(): String? {
    return try {
        PostHogJs.get_session_id()
    } catch (e: Throwable) {
        null
    }
}

internal actual fun platformOptOut() {
    try {
        PostHogJs.opt_out_capturing()
    } catch (e: Throwable) {
    }
}

internal actual fun platformOptIn() {
    try {
        PostHogJs.opt_in_capturing()
    } catch (e: Throwable) {
    }
}

internal actual fun platformIsOptedOut(): Boolean {
    return try {
        PostHogJs.has_opted_out_capturing()
    } catch (e: Throwable) {
        false
    }
}

internal actual fun platformFlush() {
    try {
        PostHogJs.flush()
    } catch (e: Throwable) {
    }
}

internal actual fun platformClose() {
    try {
        PostHogJs.shutdown()
    } catch (e: Throwable) {
    }
}

internal actual fun platformSetDebug(enabled: Boolean) {
    try {
        PostHogJs.debug(enabled)
    } catch (e: Throwable) {
    }
}

// ==================== Helper Functions ====================

private fun Map<String, Any?>.toJsObject(): dynamic {
    val obj = js("{}")
    this.forEach { (key, value) ->
        obj[key] = when (value) {
            is Map<*, *> -> (value as Map<String, Any?>).toJsObject()
            is List<*> -> value.map { item ->
                when (item) {
                    is Map<*, *> -> (item as Map<String, Any?>).toJsObject()
                    else -> item
                }
            }.toTypedArray()
            else -> value
        }
    }
    return obj
}

internal actual fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    PostHogJs.setPersonProperties(
        userProperties?.toJsObject() ?: js("{}"),
        userPropertiesSetOnce?.toJsObject()
    )
}