@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("posthog-js")
@file:Suppress("FunctionNaming", "MatchingDeclarationName")

package com.posthog.kmp

internal external interface PostHogJsApi : JsAny {
    fun init(apiKey: String, options: JsAny): PostHogJsApi
    fun capture(event: String, properties: JsAny? = definedExternally, options: JsAny? = definedExternally)
    fun identify(
        distinctId: String,
        userProperties: JsAny? = definedExternally,
        userPropertiesSetOnce: JsAny? = definedExternally
    )
    fun alias(alias: String)
    fun reset()
    fun get_distinct_id(): String
    fun register(properties: JsAny)
    fun unregister(key: String)
    fun group(type: String, key: String, groupProperties: JsAny? = definedExternally)
    fun isFeatureEnabled(key: String, options: JsAny? = definedExternally): Boolean?
    fun getFeatureFlag(key: String, options: JsAny? = definedExternally): JsAny?
    fun getAllFeatureFlags(): JsAny
    fun reloadFeatureFlags()
    fun onFeatureFlags(callback: () -> Unit): JsAny
    fun getFeatureFlagResult(key: String, options: JsAny? = definedExternally): JsAny?
    fun captureException(error: JsAny, additionalProperties: JsAny? = definedExternally)
    fun get_property(key: String): JsAny?
    fun get_session_id(): String
    fun opt_out_capturing()
    fun opt_in_capturing()
    fun has_opted_out_capturing(): Boolean
    fun shutdown(): JsAny?
    fun debug(enabled: Boolean)
    fun setPersonProperties(
        userPropertiesToSet: JsAny,
        userPropertiesToSetOnce: JsAny? = definedExternally
    )
}

internal external val posthog: PostHogJsApi
