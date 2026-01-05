package io.github.samuolis.posthog

/**
 * WebAssembly JavaScript implementation using the official posthog-js library.
 *
 * This implementation uses Kotlin/Wasm JS interop to wrap the posthog-js npm package.
 */

// External declarations for WasmJS interop
@OptIn(ExperimentalWasmJsInterop::class)
private external interface PostHogJsApi {
    fun init(apiKey: String, options: JsAny)
    fun capture(event: String, properties: JsAny? = definedExternally)
    fun identify(distinctId: String, properties: JsAny? = definedExternally)
    fun alias(alias: String)
    fun reset(resetDeviceId: Boolean = definedExternally)
    fun register(properties: JsAny)
    fun unregister(key: String)
    fun group(groupType: String, groupKey: String, groupProperties: JsAny? = definedExternally)
    fun isFeatureEnabled(key: String): Boolean
    fun getFeatureFlag(key: String): JsAny?
    fun getFeatureFlagPayload(key: String): JsAny?
    fun getAllFlags(): JsAny?
    fun reloadFeatureFlags()
    fun onFeatureFlags(callback: () -> Unit)
    fun overrideFeatureFlags(flags: JsAny)
    fun opt_out_capturing()
    fun opt_in_capturing()
    fun has_opted_out_capturing(): Boolean
    fun getDistinctId(): String
    fun flush()
    fun shutdown()
    fun debug(enabled: Boolean = definedExternally)
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("posthog-js")
@JsName("default")
private external val PostHogWasm: PostHogJsApi

// Window interface for setTimeout
@OptIn(ExperimentalWasmJsInterop::class)
private external interface Window {
    fun setTimeout(callback: () -> Unit, delay: Int)
}

@OptIn(ExperimentalWasmJsInterop::class)
private external val window: Window

// Navigator interface for online status
@OptIn(ExperimentalWasmJsInterop::class)
private external interface Navigator {
    val onLine: Boolean
}

@OptIn(ExperimentalWasmJsInterop::class)
private external val navigator: Navigator

private var isPostHogInitialized = false
private var currentConfig: PostHogConfig? = null

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val options = createJsObject()
    setJsProperty(options, "api_host", config.host.toJsString())
    setJsProperty(options, "debug", config.debug.toJsBoolean())
    setJsProperty(options, "capture_pageview", config.captureScreenViews.toJsBoolean())
    setJsProperty(options, "capture_pageleave", config.captureScreenViews.toJsBoolean())
    setJsProperty(options, "autocapture", config.autocapture.toJsBoolean())
    setJsProperty(options, "persistence", "localStorage".toJsString())
    setJsProperty(options, "advanced_disable_feature_flags", (!config.preloadFeatureFlags).toJsBoolean())

    PostHogWasm.init(config.apiKey, options)
    isPostHogInitialized = true
    currentConfig = config
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformCapture(event: String, properties: Map<String, Any?>?) {
    if (!isPostHogInitialized || !isOnline()) return

    // Defer to avoid blocking main thread
    window.setTimeout({
        try {
            if (properties != null) {
                PostHogWasm.capture(event, properties.toJsObject())
            } else {
                PostHogWasm.capture(event)
            }
        } catch (_: Throwable) {
            // Silently ignore
        }
    }, 0)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    if (!isPostHogInitialized || !isOnline()) return

    window.setTimeout({
        try {
            val screenProperties = buildMap {
                put("\$screen_name", screenName)
                properties?.forEach { (key, value) -> put(key, value) }
            }
            PostHogWasm.capture("\$screen", screenProperties.toJsObject())
        } catch (_: Throwable) {
            // Silently ignore
        }
    }, 0)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
) {
    if (!isPostHogInitialized) return

    window.setTimeout({
        try {
            if (userProperties != null) {
                PostHogWasm.identify(distinctId, userProperties.toJsObject())
            } else {
                PostHogWasm.identify(distinctId)
            }
        } catch (_: Throwable) {
            // Silently ignore
        }
    }, 0)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformAlias(alias: String) {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.alias(alias)
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformReset() {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.reset(true)
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformGetDistinctId(): String? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogWasm.getDistinctId()
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformRegister(key: String, value: Any?) {
    if (!isPostHogInitialized) return

    try {
        val props = createJsObject()
        setJsProperty(props, key, value.toJsAny())
        PostHogWasm.register(props)
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformUnregister(key: String) {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.unregister(key)
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    if (!isPostHogInitialized) return

    try {
        if (groupProperties != null) {
            PostHogWasm.group(type, key, groupProperties.toJsObject())
        } else {
            PostHogWasm.group(type, key)
        }
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean): Boolean {
    if (!isPostHogInitialized) return defaultValue

    return try {
        PostHogWasm.isFeatureEnabled(key)
    } catch (_: Throwable) {
        defaultValue
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformGetFeatureFlag(key: String): Any? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogWasm.getFeatureFlag(key)
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformGetFeatureFlagPayload(key: String): Any? {
    if (!isPostHogInitialized) return null

    return try {
        PostHogWasm.getFeatureFlagPayload(key)
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    if (!isPostHogInitialized) return emptyMap()

    return try {
        // WasmJS interop for maps is complex, returning empty for now
        // Users should use getFeatureFlag for specific flags
        emptyMap()
    } catch (_: Throwable) {
        emptyMap()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    if (!isPostHogInitialized) {
        callback?.invoke()
        return
    }

    try {
        if (callback != null) {
            PostHogWasm.onFeatureFlags { callback() }
        }
        PostHogWasm.reloadFeatureFlags()
    } catch (_: Throwable) {
        callback?.invoke()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.overrideFeatureFlags(flags.toJsObject())
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformOptOut() {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.opt_out_capturing()
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformOptIn() {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.opt_in_capturing()
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformIsOptedOut(): Boolean {
    if (!isPostHogInitialized) return false

    return try {
        PostHogWasm.has_opted_out_capturing()
    } catch (_: Throwable) {
        false
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformFlush() {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.flush()
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformClose() {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.shutdown()
        isPostHogInitialized = false
        currentConfig = null
    } catch (_: Throwable) {
        // Silently ignore
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun platformSetDebug(enabled: Boolean) {
    if (!isPostHogInitialized) return

    try {
        PostHogWasm.debug(enabled)
    } catch (_: Throwable) {
        // Silently ignore
    }
}

// ==================== Helper Functions ====================

@OptIn(ExperimentalWasmJsInterop::class)
private fun isOnline(): Boolean {
    return try {
        navigator.onLine
    } catch (_: Throwable) {
        true // Assume online if we can't check
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createJsObject(): JsAny = js("({})")

@OptIn(ExperimentalWasmJsInterop::class)
private fun setJsProperty(obj: JsAny, key: String, value: JsAny?) {
    js("obj[key] = value")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun Any?.toJsAny(): JsAny? {
    return when (this) {
        null -> null
        is String -> this.toJsString()
        is Int -> this.toJsNumber()
        is Long -> this.toDouble().toJsNumber()
        is Double -> this.toJsNumber()
        is Float -> this.toDouble().toJsNumber()
        is Boolean -> this.toJsBoolean()
        is Map<*, *> -> (this as Map<String, Any?>).toJsObject()
        else -> this.toString().toJsString()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun Map<String, Any?>.toJsObject(): JsAny {
    val result = createJsObject()
    this.forEach { (key, value) ->
        setJsProperty(result, key, value.toJsAny())
    }
    return result
}

// Helper functions for WasmJS primitives
@OptIn(ExperimentalWasmJsInterop::class)
private fun Int.toJsNumber(): JsAny = toJsNumberHelper(this.toDouble())

@OptIn(ExperimentalWasmJsInterop::class)
private fun Double.toJsNumber(): JsAny = toJsNumberHelper(this)

@OptIn(ExperimentalWasmJsInterop::class)
private fun toJsNumberHelper(value: Double): JsAny = js("value")

@OptIn(ExperimentalWasmJsInterop::class)
private fun Boolean.toJsBoolean(): JsAny = toJsBooleanHelper(this)

@OptIn(ExperimentalWasmJsInterop::class)
private fun toJsBooleanHelper(value: Boolean): JsAny = js("value")

@OptIn(ExperimentalWasmJsInterop::class)
private fun String.toJsString(): JsAny = toJsStringHelper(this)

@OptIn(ExperimentalWasmJsInterop::class)
private fun toJsStringHelper(value: String): JsAny = js("value")
