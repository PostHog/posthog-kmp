@file:OptIn(ExperimentalJsExport::class)

package com.samuolis.posthog

import kotlin.js.Promise

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
private external object PostHogJs {
    fun init(apiKey: String, options: dynamic = definedExternally)
    fun capture(event: String, properties: dynamic = definedExternally)
    fun identify(distinctId: String, properties: dynamic = definedExternally)
    fun alias(alias: String)
    fun reset(resetDeviceId: Boolean = definedExternally)
    fun register(properties: dynamic)
    fun unregister(key: String)
    fun registerOnce(properties: dynamic)
    fun group(groupType: String, groupKey: String, groupProperties: dynamic = definedExternally)
    fun isFeatureEnabled(key: String, options: dynamic = definedExternally): Boolean
    fun getFeatureFlag(key: String): Any?
    fun getFeatureFlagPayload(key: String): Any?
    fun getAllFlags(): dynamic
    fun reloadFeatureFlags()
    fun onFeatureFlags(callback: () -> Unit)
    fun overrideFeatureFlags(flags: dynamic)
    fun opt_out_capturing()
    fun opt_in_capturing()
    fun has_opted_out_capturing(): Boolean
    fun getDistinctId(): String
    fun flush()
    fun shutdown()
    fun debug(enabled: Boolean = definedExternally)
}

private var isPostHogInitialized = false
private var currentConfig: PostHogConfig? = null

@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val options = js("{}")
    options["api_host"] = config.host
    options["debug"] = config.debug
    options["capture_pageview"] = config.captureScreenViews
    options["capture_pageleave"] = config.captureScreenViews
    options["autocapture"] = config.autocapture
    options["persistence"] = "localStorage"
    options["bootstrap"] = js("{}")

    // Session recording config
    config.sessionRecording?.let { sessionConfig ->
        if (sessionConfig.enabled) {
            options["session_recording"] = js("{}")
            options["session_recording"]["maskAllInputs"] = sessionConfig.maskAllTextInputs
            options["session_recording"]["maskAllImages"] = sessionConfig.maskAllImages
        }
    }

    // Feature flags config
    options["advanced_disable_feature_flags"] = !config.preloadFeatureFlags

    PostHogJs.init(config.apiKey, options)
    isPostHogInitialized = true
    currentConfig = config
}

internal actual fun platformCapture(event: String, properties: Map<String, Any?>?) {
    if (!isPostHogInitialized) return

    try {
        if (properties != null) {
            PostHogJs.capture(event, properties.toJsObject())
        } else {
            PostHogJs.capture(event)
        }
    } catch (e: Throwable) {
        logDebug("capture error: ${e.message}")
    }
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    if (!isPostHogInitialized) return

    try {
        val screenProperties = buildMap {
            put("\$screen_name", screenName)
            properties?.forEach { (key, value) -> put(key, value) }
        }
        PostHogJs.capture("\$screen", screenProperties.toJsObject())
    } catch (e: Throwable) {
        logDebug("screen error: ${e.message}")
    }
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
) {
    if (!isPostHogInitialized) return

    try {
        val props = js("{}")
        userProperties?.forEach { (key, value) ->
            props[key] = value
        }

        PostHogJs.identify(distinctId, props)

        userPropertiesSetOnce?.let {
            val setOnceProps = js("{}")
            setOnceProps["\$set_once"] = it.toJsObject()
            PostHogJs.capture("\$identify", setOnceProps)
        }
    } catch (e: Throwable) {
        logDebug("identify error: ${e.message}")
    }
}

internal actual fun platformAlias(alias: String) {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.alias(alias)
    } catch (e: Throwable) {
        logDebug("alias error: ${e.message}")
    }
}

internal actual fun platformReset() {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.reset(true)
    } catch (e: Throwable) {
        logDebug("reset error: ${e.message}")
    }
}

internal actual fun platformGetDistinctId(): String? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogJs.getDistinctId()
    } catch (e: Throwable) {
        logDebug("getDistinctId error: ${e.message}")
        null
    }
}

internal actual fun platformRegister(key: String, value: Any?) {
    if (!isPostHogInitialized) return

    try {
        val props = js("{}")
        props[key] = value
        PostHogJs.register(props)
    } catch (e: Throwable) {
        logDebug("register error: ${e.message}")
    }
}

internal actual fun platformUnregister(key: String) {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.unregister(key)
    } catch (e: Throwable) {
        logDebug("unregister error: ${e.message}")
    }
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    if (!isPostHogInitialized) return

    try {
        if (groupProperties != null) {
            PostHogJs.group(type, key, groupProperties.toJsObject())
        } else {
            PostHogJs.group(type, key)
        }
    } catch (e: Throwable) {
        logDebug("group error: ${e.message}")
    }
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean): Boolean {
    if (!isPostHogInitialized) return defaultValue

    return try {
        PostHogJs.isFeatureEnabled(key) ?: defaultValue
    } catch (e: Throwable) {
        logDebug("isFeatureEnabled error: ${e.message}")
        defaultValue
    }
}

internal actual fun platformGetFeatureFlag(key: String): Any? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogJs.getFeatureFlag(key)
    } catch (e: Throwable) {
        logDebug("getFeatureFlag error: ${e.message}")
        null
    }
}

internal actual fun platformGetFeatureFlagPayload(key: String): Any? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogJs.getFeatureFlagPayload(key)
    } catch (e: Throwable) {
        logDebug("getFeatureFlagPayload error: ${e.message}")
        null
    }
}

internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    if (!isPostHogInitialized) return emptyMap()

    return try {
        val flags = PostHogJs.getAllFlags()
        flags.unsafeCast<Map<String, Any?>>()
    } catch (e: Throwable) {
        logDebug("getAllFeatureFlags error: ${e.message}")
        emptyMap()
    }
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (!isPostHogInitialized) {
        callback?.invoke()
        return
    }

    try {
        if (callback != null) {
            PostHogJs.onFeatureFlags { callback() }
        }
        PostHogJs.reloadFeatureFlags()
    } catch (e: Throwable) {
        logDebug("reloadFeatureFlags error: ${e.message}")
        callback?.invoke()
    }
}

internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.overrideFeatureFlags(flags.toJsObject())
    } catch (e: Throwable) {
        logDebug("overrideFeatureFlags error: ${e.message}")
    }
}

internal actual fun platformOptOut() {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.opt_out_capturing()
    } catch (e: Throwable) {
        logDebug("optOut error: ${e.message}")
    }
}

internal actual fun platformOptIn() {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.opt_in_capturing()
    } catch (e: Throwable) {
        logDebug("optIn error: ${e.message}")
    }
}

internal actual fun platformIsOptedOut(): Boolean {
    if (!isPostHogInitialized) return false

    return try {
        PostHogJs.has_opted_out_capturing()
    } catch (e: Throwable) {
        logDebug("isOptedOut error: ${e.message}")
        false
    }
}

internal actual fun platformFlush() {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.flush()
    } catch (e: Throwable) {
        logDebug("flush error: ${e.message}")
    }
}

internal actual fun platformClose() {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.shutdown()
        isPostHogInitialized = false
        currentConfig = null
    } catch (e: Throwable) {
        logDebug("close error: ${e.message}")
    }
}

internal actual fun platformSetDebug(enabled: Boolean) {
    if (!isPostHogInitialized) return

    try {
        PostHogJs.debug(enabled)
    } catch (e: Throwable) {
        logDebug("setDebug error: ${e.message}")
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

private fun logDebug(message: String) {
    if (currentConfig?.debug == true) {
        console.log("[PostHog]", message)
    }
}
