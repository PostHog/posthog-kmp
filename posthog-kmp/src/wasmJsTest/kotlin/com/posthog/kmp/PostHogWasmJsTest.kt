@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UnusedParameter")

package com.posthog.kmp

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostHogWasmJsTest {
    private lateinit var fakePostHog: PostHogJsApi

    @BeforeTest
    fun setUp() {
        fakePostHog = createFakePostHog()
        mockPostHogWasmJs = fakePostHog
    }

    @AfterTest
    fun tearDown() {
        mockPostHogWasmJs = null
    }

    @Test
    fun setupDelegatesConfigurationToPostHogJs() {
        PostHog.setup(
            PostHogConfig(
                apiKey = "phc_test",
                host = "https://example.com",
                debug = true,
                captureScreenViews = true,
                preloadFeatureFlags = false,
                optOut = true,
                personProfiles = PersonProfiles.ALWAYS,
                sessionRecording = SessionRecordingConfig(captureLogs = true),
                autocapture = true
            ),
            PostHogContext()
        )

        assertEquals("phc_test", readString(fakePostHog, "apiKey"))
        assertEquals("https://example.com", readNestedString(fakePostHog, "options", "api_host"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "debug"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "capture_pageview"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "autocapture"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "advanced_disable_feature_flags_on_first_load"))
        assertEquals("always", readNestedString(fakePostHog, "options", "person_profiles"))
        assertTrue(readDeepBoolean(fakePostHog, "options", "session_recording", "captureLogs"))
        assertTrue(readBoolean(fakePostHog, "optedOut"))
        assertEquals("posthog-kmp", readString(fakePostHog, "sdkName"))
    }

    @Test
    fun captureConvertsPropertiesGroupsAndTimestamp() {
        PostHog.capture(
            event = "checkout",
            properties = mapOf(
                "currency" to "USD",
                "amount" to 42,
                "nested" to mapOf("source" to "wasm"),
                "items" to listOf("one", "two")
            ),
            options = CaptureOptions(
                groups = mapOf("company" to "posthog"),
                timestamp = 1_700_000_000_000
            )
        )

        assertEquals("checkout", readString(fakePostHog, "event"))
        assertEquals("USD", readNestedString(fakePostHog, "properties", "currency"))
        assertEquals(42.0, readNestedNumber(fakePostHog, "properties", "amount"))
        assertEquals("wasm", readDeepString(fakePostHog, "properties", "nested", "source"))
        assertEquals("two", readArrayString(fakePostHog, "properties", "items", 1))
        assertEquals("posthog", readDeepString(fakePostHog, "properties", "\$groups", "company"))
        assertEquals(1_700_000_000_000.0, readTimestamp(fakePostHog))
    }

    @Test
    fun featureFlagResultsAreConvertedToCommonModels() {
        val result = PostHog.getFeatureFlagResult("checkout-flow", sendFeatureFlagEvent = false)
        val allResults = PostHog.getAllFeatureFlags()

        assertNotNull(result)
        assertEquals("checkout-flow", result.key)
        assertTrue(result.enabled)
        assertEquals("test", result.variant)
        assertEquals(mapOf("color" to "blue"), result.payload)
        assertFalse(readNestedBoolean(fakePostHog, "featureFlagOptions", "send_event"))
        assertEquals(result, allResults["checkout-flow"])
    }
}

private fun createFakePostHog(): PostHogJsApi = js(
    """({
        init(apiKey, options) {
            this.apiKey = apiKey;
            this.options = options;
            return this;
        },
        _overrideSDKInfo(sdkName, sdkVersion) {
            this.sdkName = sdkName;
            this.sdkVersion = sdkVersion;
        },
        opt_out_capturing() { this.optedOut = true; },
        capture(event, properties, options) {
            this.event = event;
            this.properties = properties;
            this.captureOptions = options;
        },
        getFeatureFlagResult(key, options) {
            this.featureFlagOptions = options;
            return { key: key, enabled: true, variant: 'test', payload: { color: 'blue' } };
        },
        getAllFeatureFlags() {
            return [{ key: 'checkout-flow', enabled: true, variant: 'test', payload: { color: 'blue' } }];
        }
    })"""
)

private fun readString(target: PostHogJsApi, key: String): String = js("target[key]")
private fun readBoolean(target: PostHogJsApi, key: String): Boolean = js("target[key]")
private fun readNestedString(target: PostHogJsApi, parent: String, key: String): String = js("target[parent][key]")
private fun readNestedBoolean(target: PostHogJsApi, parent: String, key: String): Boolean = js("target[parent][key]")
private fun readNestedNumber(target: PostHogJsApi, parent: String, key: String): Double = js("target[parent][key]")
private fun readDeepString(target: PostHogJsApi, parent: String, child: String, key: String): String = js("target[parent][child][key]")
private fun readDeepBoolean(target: PostHogJsApi, parent: String, child: String, key: String): Boolean = js("target[parent][child][key]")
private fun readArrayString(target: PostHogJsApi, parent: String, child: String, index: Int): String = js("target[parent][child][index]")
private fun readTimestamp(target: PostHogJsApi): Double = js("target.captureOptions.timestamp.getTime()")
