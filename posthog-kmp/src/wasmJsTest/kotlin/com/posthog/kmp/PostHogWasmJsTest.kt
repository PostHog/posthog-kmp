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

    @Test
    fun screenCapturesScreenEventWithName() {
        PostHog.screen(screenName = "Checkout", properties = mapOf("section" to "demo"))

        assertEquals("\$screen", readString(fakePostHog, "event"))
        assertEquals("Checkout", readNestedString(fakePostHog, "properties", "\$screen_name"))
        assertEquals("demo", readNestedString(fakePostHog, "properties", "section"))
    }

    @Test
    fun identifyRoutesDistinctIdAndProperties() {
        PostHog.identify(
            distinctId = "user_42",
            userProperties = mapOf("plan" to "scale"),
            userPropertiesSetOnce = mapOf("signup_source" to "wasm")
        )

        assertEquals("user_42", readString(fakePostHog, "distinctId"))
        assertEquals("scale", readNestedString(fakePostHog, "userProperties", "plan"))
        assertEquals("wasm", readNestedString(fakePostHog, "userPropertiesSetOnce", "signup_source"))
    }

    @Test
    fun flushDrainsRequestAndRetryQueues() {
        PostHog.flush()

        assertTrue(readBoolean(fakePostHog, "requestQueueUnloaded"))
        assertTrue(readBoolean(fakePostHog, "retryQueueUnloaded"))
    }

    @Test
    fun reloadFeatureFlagsCallbackIgnoresStaleImmediateFire() {
        var callbackCount = 0

        PostHog.reloadFeatureFlags { callbackCount++ }

        assertEquals(1, callbackCount)
        assertEquals(1.0, readNumber(fakePostHog, "reloadCount"))
        assertTrue(readBoolean(fakePostHog, "unsubscribed"))
    }

    @Test
    fun getAllFeatureFlagsReturnsEmptyMapWhenFlagsAreUnavailable() {
        mockPostHogWasmJs = createUninitializedFakePostHog()

        assertEquals(emptyMap(), PostHog.getAllFeatureFlags())
    }

    @Test
    fun getDistinctIdReturnsNullWhenUnavailable() {
        mockPostHogWasmJs = createUninitializedFakePostHog()

        assertEquals(null, PostHog.getDistinctId())
    }

    @Test
    fun captureExceptionPreservesTypeMessageAndStack() {
        PostHog.captureException(IllegalStateException("checkout blew up"))

        assertEquals("IllegalStateException", readNestedString(fakePostHog, "capturedError", "name"))
        assertEquals("checkout blew up", readNestedString(fakePostHog, "capturedError", "message"))
        assertTrue(readNestedString(fakePostHog, "capturedError", "stack").isNotEmpty())
    }

    @Test
    fun optOutOptInAndResetRouteToPostHogJs() {
        PostHog.optOut()
        assertTrue(readBoolean(fakePostHog, "optedOut"))

        PostHog.optIn()
        assertTrue(readBoolean(fakePostHog, "optedIn"))

        PostHog.reset()
        assertEquals(1.0, readNumber(fakePostHog, "resetCalls"))
    }
}

private fun createFakePostHog(): PostHogJsApi = js(
    """(() => {
        const fake = {
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
            opt_in_capturing() { this.optedIn = true; },
            reset() { this.resetCalls = (this.resetCalls || 0) + 1; },
            capture(event, properties, options) {
                this.event = event;
                this.properties = properties;
                this.captureOptions = options;
            },
            identify(distinctId, userProperties, userPropertiesSetOnce) {
                this.distinctId = distinctId;
                this.userProperties = userProperties;
                this.userPropertiesSetOnce = userPropertiesSetOnce;
            },
            onFeatureFlags(callback) {
                this.flagsCallback = callback;
                callback();
                return () => { this.unsubscribed = true; };
            },
            reloadFeatureFlags() {
                this.reloadCount = (this.reloadCount || 0) + 1;
                if (this.flagsCallback) this.flagsCallback();
            },
            getFeatureFlagResult(key, options) {
                this.featureFlagOptions = options;
                return { key: key, enabled: true, variant: 'test', payload: { color: 'blue' } };
            },
            getAllFeatureFlags() {
                return [{ key: 'checkout-flow', enabled: true, variant: 'test', payload: { color: 'blue' } }];
            },
            captureException(error, additionalProperties) {
                this.capturedError = error;
                this.capturedErrorProperties = additionalProperties;
            }
        };
        fake._requestQueue = { unload() { fake.requestQueueUnloaded = true; } };
        fake._retryQueue = { unload() { fake.retryQueueUnloaded = true; } };
        return fake;
    })()"""
)

private fun createUninitializedFakePostHog(): PostHogJsApi = js(
    "({ getAllFeatureFlags() { return undefined; }, get_distinct_id() { return undefined; } })"
)

private fun readString(target: PostHogJsApi, key: String): String = js("target[key]")
private fun readBoolean(target: PostHogJsApi, key: String): Boolean = js("target[key]")
private fun readNumber(target: PostHogJsApi, key: String): Double = js("target[key]")
private fun readNestedString(target: PostHogJsApi, parent: String, key: String): String = js("target[parent][key]")
private fun readNestedBoolean(target: PostHogJsApi, parent: String, key: String): Boolean = js("target[parent][key]")
private fun readNestedNumber(target: PostHogJsApi, parent: String, key: String): Double = js("target[parent][key]")
private fun readDeepString(target: PostHogJsApi, parent: String, child: String, key: String): String = js("target[parent][child][key]")
private fun readDeepBoolean(target: PostHogJsApi, parent: String, child: String, key: String): Boolean = js("target[parent][child][key]")
private fun readArrayString(target: PostHogJsApi, parent: String, child: String, index: Int): String = js("target[parent][child][index]")
private fun readTimestamp(target: PostHogJsApi): Double = js("target.captureOptions.timestamp.getTime()")
