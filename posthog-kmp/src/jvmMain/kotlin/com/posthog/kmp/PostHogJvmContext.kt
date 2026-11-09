package com.posthog.kmp

import java.util.Locale
import java.util.TimeZone

/**
 * Event context for the JVM. [getSdkInfo] supplies `$lib`/`$lib_version` on every event —
 * without it events are not attributed to this SDK.
 */
internal class PostHogJvmContext(
    private val config: com.posthog.PostHogConfig,
) : com.posthog.internal.PostHogContext {
    private val cacheStaticContext by lazy {
        buildMap<String, Any> {
            System.getProperty("os.name")?.let { put("\$os_name", normalizeOsName(it)) }
            System.getProperty("os.version")?.let { put("\$os_version", it) }
            put("\$device_type", "Desktop")
        }
    }

    // os.name on Windows includes the release ("Windows 11"); flags target plain "Windows"
    private fun normalizeOsName(raw: String): String =
        if (raw.startsWith("Windows")) "Windows" else raw

    private val cacheSdkInfo by lazy {
        mapOf<String, Any>(
            "\$lib" to config.sdkName,
            "\$lib_version" to config.sdkVersion
        )
    }

    override fun getStaticContext(): Map<String, Any> = cacheStaticContext

    override fun getDynamicContext(): Map<String, Any> =
        buildMap {
            // BCP 47, matching posthog-android: keeps the script, drops extensions such as -u-fw-mon
            Locale.getDefault().stripExtensions().toLanguageTag().takeUnless { it == "und" }?.let {
                put("\$locale", it)
            }
            put("\$timezone", TimeZone.getDefault().id)
        }

    override fun getSdkInfo(): Map<String, Any> = cacheSdkInfo
}
