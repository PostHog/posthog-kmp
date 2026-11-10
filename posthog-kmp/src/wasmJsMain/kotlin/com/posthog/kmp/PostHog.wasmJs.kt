@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UnusedParameter")

package com.posthog.kmp

/** Kotlin/Wasm implementation backed by the official posthog-js package. */

internal var mockPostHogWasmJs: PostHogJsApi? = null

private val PostHogJs: PostHogJsApi
    get() = mockPostHogWasmJs ?: posthog

@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    val options = createJsObject()
    setJsProperty(options, "api_host", config.host.toJsString())
    setJsProperty(options, "debug", config.debug.toJsBoolean())
    setJsProperty(options, "capture_pageview", config.captureScreenViews.toJsBoolean())
    setJsProperty(options, "capture_pageleave", config.captureScreenViews.toJsBoolean())
    setJsProperty(options, "autocapture", config.autocapture.toJsBoolean())
    config.errorTracking?.let {
        setJsProperty(options, "capture_exceptions", it.autoCapture.toJsBoolean())
    }
    setJsProperty(options, "persistence", "localStorage".toJsString())
    setJsProperty(options, "defaults", "2026-05-30".toJsString())
    setJsProperty(options, "person_profiles", config.personProfiles.postHogValue().toJsString())
    setJsProperty(options, "advanced_disable_feature_flags_on_first_load", (!config.preloadFeatureFlags).toJsBoolean())

    val bootstrap = createJsObject()
    setJsProperty(options, "bootstrap", bootstrap)
    config.evaluationContexts?.let { setJsProperty(options, "evaluation_contexts", it.toJsArray()) }
    if (config.beforeSend.isNotEmpty()) {
        setBeforeSendCallback(options) { captureResult -> processBeforeSend(config, captureResult) }
    }
    config.sessionRecording?.takeIf { it.enabled }?.let { configureSessionRecording(options, it) }

    PostHogJs.init(config.projectToken, options)
    overrideSdkInfo(PostHogJs, "posthog-kmp", PostHogKmpVersion.VERSION)

    if (config.optOut) {
        PostHogJs.opt_out_capturing()
    }

    pendingFeatureFlagsListeners.forEach { it.attach() }
    pendingFeatureFlagsListeners.clear()
}

private fun PersonProfiles.postHogValue(): String = when (this) {
    PersonProfiles.ALWAYS -> "always"
    PersonProfiles.IDENTIFIED_ONLY -> "identified_only"
    PersonProfiles.NEVER -> "never"
}

private fun configureSessionRecording(options: JsAny, config: SessionRecordingConfig) {
    val sessionRecording = createJsObject()
    setJsProperty(sessionRecording, "maskAllInputs", config.maskAllTextInputs.toJsBoolean())
    setJsProperty(sessionRecording, "maskAllImages", config.maskAllImages.toJsBoolean())
    setJsProperty(sessionRecording, "captureLogs", config.captureLogs.toJsBoolean())

    val networkCapture = createJsObject()
    setJsProperty(networkCapture, "recordHeaders", config.captureNetworkTelemetry.toJsBoolean())
    setJsProperty(sessionRecording, "networkCaptureConfig", networkCapture)
    setJsProperty(options, "session_recording", sessionRecording)
}

internal actual fun platformCapture(
    event: String,
    properties: Map<String, Any>?,
    groups: Map<String, String>?,
    timestamp: Long?
) {
    val options = createJsObject()
    timestamp?.let { setJsProperty(options, "timestamp", createJsDate(it.toDouble())) }

    val mergedProperties = if (properties == null && groups == null) {
        null
    } else {
        buildMap<String, Any?> {
            properties?.let { putAll(it) }
            groups?.let { put(PostHogProperties.GROUPS, it) }
        }.toJsObject()
    }

    PostHogJs.capture(event, mergedProperties, options)
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any>?) {
    val screenProperties = buildMap<String, Any?> {
        put("\$screen_name", screenName)
        properties?.let { putAll(it) }
    }
    PostHogJs.capture("\$screen", screenProperties.toJsObject())
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
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
    PostHogJs.reset()
}

internal actual fun platformGetDistinctId(): String? = wasmCall("getDistinctId") {
    PostHogJs.get_distinct_id()?.takeIf { isJsString(it) }?.toKotlinString()
}

internal actual fun platformRegister(key: String, value: Any) {
    val properties = createJsObject()
    setJsProperty(properties, key, value.toJsAny())
    PostHogJs.register(properties)
}

internal actual fun platformUnregister(key: String) {
    PostHogJs.unregister(key)
}

@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetAppLanguage(language: String?) {
    // posthog-js has no $app_language yet, so Wasm doesn't send it
}

internal actual fun platformGroup(type: String, key: String, groupProperties: Map<String, Any>?) {
    PostHogJs.group(type, key, groupProperties?.toJsObject())
}

internal actual fun platformIsFeatureEnabled(
    key: String,
    defaultValue: Boolean,
    sendFeatureFlagEvent: Boolean
): Boolean {
    val options = featureFlagOptions(sendFeatureFlagEvent)
    return PostHogJs.isFeatureEnabled(key, options)?.toKotlinBoolean() ?: defaultValue
}

internal actual fun platformGetFeatureFlag(key: String, sendFeatureFlagEvent: Boolean): Any? {
    val value = PostHogJs.getFeatureFlag(key, featureFlagOptions(sendFeatureFlagEvent)) ?: return null
    return value.toKotlinValue()
}

private fun featureFlagOptions(sendFeatureFlagEvent: Boolean): JsAny {
    val options = createJsObject()
    setJsProperty(options, "send_event", sendFeatureFlagEvent.toJsBoolean())
    return options
}

internal actual fun platformGetAllFeatureFlags(): Map<String, FeatureFlagResult> {
    val results = wasmCall("getAllFeatureFlags") { PostHogJs.getAllFeatureFlags() } ?: return emptyMap()
    if (!isJsArray(results)) return emptyMap()
    return buildMap {
        repeat(jsArrayLength(results)) { index ->
            val result = jsArrayItem(results, index) ?: return@repeat
            result.toFeatureFlagResult()?.let { put(it.key, it) }
        }
    }
}

internal actual fun platformReloadFeatureFlags(callback: ((PostHogFeatureFlagsLoaded) -> Unit)?) {
    if (callback == null) {
        PostHogJs.reloadFeatureFlags()
        return
    }
    if (!isPostHogJsLoaded(PostHogJs)) {
        callback(PostHogFeatureFlagsLoaded(emptyMap(), errorsLoading = true))
        return
    }

    var registered = false
    var done = false
    var unsubscribe: JsAny? = null
    unsubscribe = PostHogJs.onFeatureFlags { _, variants, context ->
        if (registered && !done) {
            done = true
            unsubscribe?.let(::invokeJsFunction)
            callback(featureFlagsLoaded(variants, context))
        }
    }
    registered = true
    PostHogJs.reloadFeatureFlags()
}

/** [PostHog.onFeatureFlags] registrations made before setup; posthog-js drops them until it's initialized. */
private val pendingFeatureFlagsListeners = mutableListOf<FeatureFlagsListener>()

private class FeatureFlagsListener(private val callback: (PostHogFeatureFlagsLoaded) -> Unit) {
    private var unsubscribe: JsAny? = null
    private var active = true

    fun attach() {
        if (!active) return
        // posthog-js iterates a snapshot of its listeners, so one unsubscribed mid-delivery still gets called
        unsubscribe = PostHogJs.onFeatureFlags { _, variants, context ->
            if (active) callback(featureFlagsLoaded(variants, context))
        }
    }

    fun detach() {
        active = false
        pendingFeatureFlagsListeners.remove(this)
        unsubscribe?.let(::invokeJsFunction)
        unsubscribe = null
    }
}

internal actual fun platformOnFeatureFlags(callback: (PostHogFeatureFlagsLoaded) -> Unit): PostHogFeatureFlagsSubscription {
    val listener = FeatureFlagsListener(callback)
    if (isPostHogJsLoaded(PostHogJs)) listener.attach() else pendingFeatureFlagsListeners.add(listener)
    return PostHogFeatureFlagsSubscription { listener.detach() }
}

private fun featureFlagsLoaded(variants: JsAny?, context: JsAny?): PostHogFeatureFlagsLoaded = PostHogFeatureFlagsLoaded(
    featureFlags = variants?.takeIf { isPlainJsObject(it) }?.toKotlinMap().orEmpty(),
    errorsLoading = context?.let { getJsProperty(it, "errorsLoading") }?.takeIf { isJsBoolean(it) }?.toKotlinBoolean()
        ?: latestFeatureFlagsLoadFailed()
)

/** The posthog-js property that holds the errors of the latest flag load. */
private const val FEATURE_FLAG_ERRORS_PROPERTY = "\$feature_flag_errors"

/** Flags loaded, but some couldn't be computed; posthog-js reports that load with `errorsLoading = false`. */
private const val ERRORS_WHILE_COMPUTING_FLAGS = "errors_while_computing_flags"

/**
 * posthog-js leaves `errorsLoading` out when it replays cached flags to a new listener, and when
 * flags change without a load. It stores the errors of its latest load, so read them instead.
 */
private fun latestFeatureFlagsLoadFailed(): Boolean = wasmCall("onFeatureFlags") {
    val errors = PostHogJs.get_property(FEATURE_FLAG_ERRORS_PROPERTY)?.takeIf { isJsArray(it) } ?: return@wasmCall false
    (0 until jsArrayLength(errors)).any { index ->
        jsArrayItem(errors, index)?.takeIf { isJsString(it) }?.toKotlinString() != ERRORS_WHILE_COMPUTING_FLAGS
    }
} ?: false

internal actual fun platformSetPersonPropertiesForFlags(properties: Map<String, Any>, reloadFeatureFlags: Boolean) {
    if (!isPostHogJsLoaded(PostHogJs)) return
    PostHogJs.setPersonPropertiesForFlags(properties.toJsObject(), reloadFeatureFlags)
}

internal actual fun platformResetPersonPropertiesForFlags(reloadFeatureFlags: Boolean) {
    if (!isPostHogJsLoaded(PostHogJs)) return
    PostHogJs.resetPersonPropertiesForFlags(reloadFeatureFlags)
}

internal actual fun platformSetGroupPropertiesForFlags(
    type: String,
    properties: Map<String, Any>,
    reloadFeatureFlags: Boolean
) {
    if (!isPostHogJsLoaded(PostHogJs)) return
    PostHogJs.setGroupPropertiesForFlags(mapOf(type to properties).toJsObject(), reloadFeatureFlags)
}

internal actual fun platformResetGroupPropertiesForFlags(type: String?, reloadFeatureFlags: Boolean) {
    if (!isPostHogJsLoaded(PostHogJs)) return
    // posthog-js has no reload option here and never reloads
    PostHogJs.resetGroupPropertiesForFlags(type)
    if (reloadFeatureFlags) PostHogJs.reloadFeatureFlags()
}

internal actual fun platformGetFeatureFlagResult(
    key: String,
    sendFeatureFlagEvent: Boolean
): FeatureFlagResult? {
    return PostHogJs.getFeatureFlagResult(key, featureFlagOptions(sendFeatureFlagEvent))?.toFeatureFlagResult()
}

private fun JsAny.toFeatureFlagResult(): FeatureFlagResult? {
    val key = getJsProperty(this, "key")?.toKotlinString() ?: return null
    val enabled = getJsProperty(this, "enabled")?.toKotlinBoolean() ?: false
    val variant = getJsProperty(this, "variant")?.toKotlinString()
    val payload = getJsProperty(this, "payload")?.toKotlinValue()
    return featureFlagResult(key = key, enabled = enabled, variant = variant, payload = payload)
}

private fun processBeforeSend(config: PostHogConfig, captureResult: JsAny?): JsAny? {
    captureResult ?: return null
    return try {
        val event = captureResult.toPostHogEvent() ?: return null
        val processed = config.runBeforeSend(event) {
            if (config.debug) logWasmError("beforeSend", "callback failed; event was dropped")
        } ?: return null

        val processedProperties = processed.properties.toJsObject()
        setJsProperty(processedProperties, "distinct_id", processed.distinctId.toJsString())
        setJsProperty(captureResult, "event", processed.event.toJsString())
        setJsProperty(captureResult, "properties", processedProperties)
        captureResult
    } catch (_: Throwable) {
        if (config.debug) logWasmError("beforeSend", "processing failed; event was dropped")
        null
    }
}

private fun JsAny.toPostHogEvent(): PostHogEvent? {
    val event = getJsProperty(this, "event")?.toKotlinString() ?: return null
    val jsProperties = getJsProperty(this, "properties") ?: return null
    val properties = jsProperties.toKotlinMap()
    val distinctId = properties["distinct_id"] as? String ?: return null
    return PostHogEvent(event, distinctId, properties - "distinct_id")
}

internal actual fun platformCaptureException(throwable: Throwable, additionalProperties: Map<String, Any>?) {
    PostHogJs.captureException(
        createJsError(
            throwable::class.simpleName ?: "Error",
            throwable.message ?: throwable.toString(),
            throwable.stackTraceToString()
        ),
        additionalProperties?.toJsObject()
    )
}

internal actual fun platformGetAnonymousId(): String? {
    return PostHogJs.get_property("\$device_id")?.toKotlinString()
}

internal actual fun platformGetSessionId(): String? = wasmCall("getSessionId") {
    PostHogJs.get_session_id()
}

internal actual fun platformOptOut() {
    wasmCall("optOut") { PostHogJs.opt_out_capturing() }
}

internal actual fun platformOptIn() {
    wasmCall("optIn") { PostHogJs.opt_in_capturing() }
}

internal actual fun platformIsOptedOut(): Boolean = wasmCall("isOptedOut") {
    PostHogJs.has_opted_out_capturing()
} ?: false

internal actual fun platformFlush() {
    wasmCall("flush") { flushPostHogQueues(PostHogJs) }
}

internal actual fun platformClose() {
    wasmCall("close") { PostHogJs.shutdown() }
}

internal actual fun platformSetDebug(enabled: Boolean) {
    wasmCall("setDebug") { PostHogJs.debug(enabled) }
}

internal actual fun platformSetPersonProperties(
    userProperties: Map<String, Any>?,
    userPropertiesSetOnce: Map<String, Any>?
) {
    PostHogJs.setPersonProperties(
        userProperties?.toJsObject() ?: createJsObject(),
        userPropertiesSetOnce?.toJsObject()
    )
}

private inline fun <T> wasmCall(operation: String, call: () -> T): T? {
    return try {
        call()
    } catch (error: Throwable) {
        logWasmError(operation, error.message ?: error.toString())
        null
    }
}

private fun Map<String, Any?>.toJsObject(): JsAny {
    val result = createJsObject()
    forEach { (key, value) -> setJsProperty(result, key, value.toJsAny()) }
    return result
}

private fun List<*>.toJsArray(): JsAny {
    val result = createJsArray()
    forEach { appendJsArray(result, it.toJsAny()) }
    return result
}

private fun Any?.toJsAny(): JsAny? = when (this) {
    null -> null
    is String -> toJsString()
    is Boolean -> toJsBoolean()
    is Number -> numberToJsAny(toDouble())
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        (this as Map<String, Any?>).toJsObject()
    }
    is List<*> -> toJsArray()
    is OpaqueJsValue -> value
    else -> toString().toJsString()
}

private fun JsAny.toKotlinValue(): Any? = when {
    isJsString(this) -> toKotlinString()
    isJsBoolean(this) -> toKotlinBoolean()
    isJsNumber(this) -> toKotlinDouble()
    isJsArray(this) -> List(jsArrayLength(this)) { index -> jsArrayItem(this, index)?.toKotlinValue() }
    isPlainJsObject(this) -> toKotlinMap()
    else -> OpaqueJsValue(this)
}

private class OpaqueJsValue(val value: JsAny)

private fun JsAny.toKotlinMap(): Map<String, Any?> {
    val keys = jsObjectKeys(this)
    return buildMap {
        repeat(jsArrayLength(keys)) { index ->
            val key = jsArrayItem(keys, index)?.toKotlinString() ?: return@repeat
            val value = getJsProperty(this@toKotlinMap, key)
            if (value != null) {
                put(key, value.toKotlinValue())
            } else if (isJsNullProperty(this@toKotlinMap, key)) {
                put(key, null)
            }
        }
    }
}

private fun createJsObject(): JsAny = js("({})")
private fun createJsArray(): JsAny = js("([])")
private fun createJsDate(timestamp: Double): JsAny = js("new Date(timestamp)")
private fun createJsError(name: String, message: String, stack: String): JsAny =
    js("Object.assign(new Error(message), { name: name, stack: stack })")
private fun numberToJsAny(value: Double): JsAny = js("value")
private fun setJsProperty(target: JsAny, key: String, value: JsAny?): Unit = js("{ target[key] = value; }")
private fun setBeforeSendCallback(target: JsAny, callback: (JsAny?) -> JsAny?): Unit =
    js("{ target.before_send = callback; }")
private fun getJsProperty(target: JsAny, key: String): JsAny? = js("target[key]")
private fun appendJsArray(target: JsAny, value: JsAny?): Unit = js("{ target.push(value); }")
private fun jsArrayLength(value: JsAny): Int = js("value.length")
private fun jsArrayItem(value: JsAny, index: Int): JsAny? = js("value[index]")
private fun jsObjectKeys(value: JsAny): JsAny = js("Object.keys(value)")
private fun isJsArray(value: JsAny): Boolean = js("Array.isArray(value)")
private fun isJsString(value: JsAny): Boolean = js("typeof value === 'string'")
private fun isJsBoolean(value: JsAny): Boolean = js("typeof value === 'boolean'")
private fun isJsNumber(value: JsAny): Boolean = js("typeof value === 'number'")
private fun isPlainJsObject(value: JsAny): Boolean =
    js("typeof value === 'object' && value !== null && (Object.getPrototypeOf(value) === Object.prototype || Object.getPrototypeOf(value) === null)")
private fun isJsNullProperty(target: JsAny, key: String): Boolean = js("target[key] === null")
private fun JsAny.toKotlinString(): String = jsStringValue(this)
private fun JsAny.toKotlinBoolean(): Boolean = jsBooleanValue(this)
private fun JsAny.toKotlinDouble(): Double = jsNumberValue(this)
private fun jsStringValue(value: JsAny): String = js("value")
private fun jsBooleanValue(value: JsAny): Boolean = js("value")
private fun jsNumberValue(value: JsAny): Double = js("value")
private fun invokeJsFunction(callable: JsAny): Unit = js("{ callable(); }")
/** posthog-js sets `__loaded` in `init()`; before it, flag calls reach no persistence. */
private fun isPostHogJsLoaded(instance: PostHogJsApi): Boolean = js("instance.__loaded === true")
private fun overrideSdkInfo(instance: PostHogJsApi, sdkName: String, sdkVersion: String): Unit =
    js("{ instance._overrideSDKInfo?.(sdkName, sdkVersion); }")
private fun flushPostHogQueues(instance: PostHogJsApi): Unit = js("{ instance._requestQueue?.unload(); instance._retryQueue?.unload(); }")
private fun logWasmError(operation: String, message: String): Unit = js("{ console.error('[PostHog] ' + operation + ' failed', message); }")
