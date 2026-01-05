package com.samuolis.posthog

/**
 * macOS implementation - stub for now.
 *
 * Native PostHog SDK support can be added later when SPM4KMP properly
 * supports macOS 10.15+ deployment targets.
 *
 * TODO: Implement using Ktor HTTP client or native SDK when available.
 */

private var currentConfig: PostHogConfig? = null

@Suppress("UNUSED_PARAMETER")
internal actual fun platformSetup(config: PostHogConfig, context: PostHogContext) {
    currentConfig = config
    if (config.debug) {
        println("[PostHog] macOS: Initialized (stub implementation)")
    }
}

internal actual fun platformCapture(event: String, properties: Map<String, Any?>?) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: capture($event) - not implemented")
    }
}

internal actual fun platformScreen(screenName: String, properties: Map<String, Any?>?) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: screen($screenName) - not implemented")
    }
}

internal actual fun platformIdentify(
    distinctId: String,
    userProperties: Map<String, Any?>?,
    userPropertiesSetOnce: Map<String, Any?>?
) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: identify($distinctId) - not implemented")
    }
}

internal actual fun platformAlias(alias: String) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: alias($alias) - not implemented")
    }
}

internal actual fun platformReset() {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: reset() - not implemented")
    }
}

internal actual fun platformGetDistinctId(): String? {
    return null
}

internal actual fun platformRegister(key: String, value: Any?) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: register($key) - not implemented")
    }
}

internal actual fun platformUnregister(key: String) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: unregister($key) - not implemented")
    }
}

internal actual fun platformGroup(
    type: String,
    key: String,
    groupProperties: Map<String, Any?>?
) {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: group($type, $key) - not implemented")
    }
}

internal actual fun platformIsFeatureEnabled(key: String, defaultValue: Boolean): Boolean {
    return defaultValue
}

internal actual fun platformGetFeatureFlag(key: String): Any? {
    return null
}

internal actual fun platformGetFeatureFlagPayload(key: String): Any? {
    return null
}

internal actual fun platformGetAllFeatureFlags(): Map<String, Any?> {
    return emptyMap()
}

internal actual fun platformReloadFeatureFlags(callback: (() -> Unit)?) {
    callback?.invoke()
}

internal actual fun platformOverrideFeatureFlags(flags: Map<String, Any?>) {
    // Not implemented
}

internal actual fun platformOptOut() {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: optOut() - not implemented")
    }
}

internal actual fun platformOptIn() {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: optIn() - not implemented")
    }
}

internal actual fun platformIsOptedOut(): Boolean {
    return false
}

internal actual fun platformFlush() {
    if (currentConfig?.debug == true) {
        println("[PostHog] macOS: flush() - not implemented")
    }
}

internal actual fun platformClose() {
    currentConfig = null
}

internal actual fun platformSetDebug(enabled: Boolean) {
    // Not implemented
}
