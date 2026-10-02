@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UnusedParameter")

package com.posthog.kmp

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostHogWasmJsTest {
    private lateinit var fakePostHog: PostHogJsApi

    @BeforeTest
    fun setUp() {
        fakePostHog = createFakePostHog()
        mockPostHogWasmJs = fakePostHog
        currentConfig = null
    }

    @AfterTest
    fun tearDown() {
        mockPostHogWasmJs = null
        currentConfig = null
    }

    @Test
    fun setupDelegatesConfigurationToPostHogJs() {
        PostHog.setup(
            PostHogConfig(
                projectToken = "phc_test",
                host = "https://example.com",
                debug = true,
                captureScreenViews = true,
                preloadFeatureFlags = false,
                optOut = true,
                personProfiles = PersonProfiles.ALWAYS,
                sessionRecording = SessionRecordingConfig(captureLogs = true),
                autocapture = true,
                errorTracking = ErrorTrackingConfig(autoCapture = true)
            ),
            PostHogContext()
        )

        assertEquals("phc_test", readString(fakePostHog, "apiKey"))
        assertEquals("https://example.com", readNestedString(fakePostHog, "options", "api_host"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "debug"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "capture_pageview"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "autocapture"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "capture_exceptions"))
        assertTrue(readNestedBoolean(fakePostHog, "options", "advanced_disable_feature_flags_on_first_load"))
        assertEquals("always", readNestedString(fakePostHog, "options", "person_profiles"))
        assertTrue(readDeepBoolean(fakePostHog, "options", "session_recording", "captureLogs"))
        assertTrue(readBoolean(fakePostHog, "optedOut"))
        assertEquals("posthog-kmp", readString(fakePostHog, "sdkName"))
    }

    @Test
    fun setupMapsBeforeSendToPostHogJs() {
        PostHog.setup(
            PostHogConfig(
                projectToken = "phc_test",
                beforeSend = listOf(
                    PostHogBeforeSend {
                        it.copy(
                            event = "sanitized",
                            distinctId = "anonymous",
                            properties = it.properties - "email"
                        )
                    }
                )
            ),
            PostHogContext()
        )

        val result = assertNotNull(invokeBeforeSend(fakePostHog))

        assertEquals("sanitized", readJsString(result, "event"))
        assertEquals("anonymous", readDeepJsString(result, "properties", "distinct_id"))
        assertEquals("paid", readDeepJsString(result, "properties", "plan"))
        assertFalse(hasDeepJsProperty(result, "properties", "email"))
        assertTrue(hasDeepJsProperty(result, "properties", "nullable"))
        assertTrue(isDeepJsNull(result, "properties", "nullable"))
        assertEquals("two", readDeepJsArrayString(result, "properties", "nested", 1, 0))
        assertEquals(0.0, readDeepJsDateTime(result, "properties", "date"))
        assertNull(invokeBeforeSendWithoutDistinctId(fakePostHog))
        assertNull(invokeBeforeSendWithoutEvent(fakePostHog))
    }

    @Test
    fun beforeSendContainsCallbackExceptions() {
        var sentinelCalled = false
        PostHog.setup(
            PostHogConfig(
                projectToken = "phc_test",
                beforeSend = listOf(
                    PostHogBeforeSend { it.copy(event = "transformed") },
                    PostHogBeforeSend { throw IllegalStateException("failed") },
                    PostHogBeforeSend {
                        sentinelCalled = true
                        it
                    }
                )
            ),
            PostHogContext()
        )

        assertNull(invokeBeforeSend(fakePostHog))
        assertFalse(sentinelCalled)
    }

    @Test
    fun beforeSendContainsPropertyConversionExceptions() {
        var callbackCalled = false
        PostHog.setup(
            PostHogConfig(projectToken = "key", beforeSend = listOf(PostHogBeforeSend {
                callbackCalled = true
                it
            })),
            PostHogContext()
        )

        assertNull(invokeBeforeSendWithThrowingGetter(fakePostHog))
        assertFalse(callbackCalled)
    }

    @Test
    fun captureDropsNullPropertyKeys() {
        PostHog.capture("checkout", mapOf("keep" to true, "drop" to null))
        assertTrue(readNestedBoolean(fakePostHog, "properties", "keep"))
        assertFalse(hasNestedProperty(fakePostHog, "properties", "drop"))
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
        assertEquals("2023-11-14T22:13:20.000Z", readTimestampIso(fakePostHog))
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

        assertEquals(0, callbackCount, "registration must not deliver stale flags")
        assertEquals(1.0, readNumber(fakePostHog, "reloadCount"))
        assertFalse(readBoolean(fakePostHog, "unsubscribed"))

        notifyFeatureFlags(fakePostHog)
        assertEquals(1, callbackCount)
        assertTrue(readBoolean(fakePostHog, "unsubscribed"))

        notifyFeatureFlags(fakePostHog)
        assertEquals(1, callbackCount, "an already queued notification must not fire twice")
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
    fun unknownFeatureFlagReturnsNoResult() {
        mockPostHogWasmJs = createUninitializedFakePostHog()
        assertNull(PostHog.getFeatureFlagResult("missing"))
    }

    @Test
    fun featureFlagConfiguredEventOptionIsForwarded() {
        currentConfig = PostHogConfig(projectToken = "key", sendFeatureFlagEvent = false)
        assertFalse(PostHog.isFeatureEnabled("disabled", defaultValue = true))
        assertFalse(readNestedBoolean(fakePostHog, "featureFlagOptions", "send_event"))
        assertFalse(PostHog.isFeatureEnabled("disabled", sendFeatureFlagEvent = true))
        assertTrue(readNestedBoolean(fakePostHog, "featureFlagOptions", "send_event"), "per-call override")
        assertEquals("blue", PostHog.getFeatureFlag("checkout"))
        assertFalse(readNestedBoolean(fakePostHog, "featureFlagOptions", "send_event"))
    }

    @Test
    fun identityAndPrivacyGettersReturnNativeValues() {
        assertEquals("user-42", PostHog.getDistinctId())
        assertEquals("anon-42", PostHog.getAnonymousId())
        assertEquals("session-42", PostHog.getSessionId())
        assertFalse(PostHog.isOptedOut())
        PostHog.optOut()
        assertTrue(PostHog.isOptedOut())
        PostHog.optIn()
        assertFalse(PostHog.isOptedOut())
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
            optedOut: false,
            opt_out_capturing() { this.optedOut = true; },
            opt_in_capturing() { this.optedIn = true; this.optedOut = false; },
            has_opted_out_capturing() { return this.optedOut; },
            get_distinct_id() { return 'user-42'; },
            get_property(key) { return key === '${'$'}device_id' ? 'anon-42' : undefined; },
            get_session_id() { return 'session-42'; },
            isFeatureEnabled(key, options) {
                this.featureFlagOptions = options;
                return key === 'disabled' ? false : undefined;
            },
            getFeatureFlag(key, options) {
                this.featureFlagOptions = options;
                return key === 'checkout' ? 'blue' : undefined;
            },
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
            unsubscribed: false,
            onFeatureFlags(callback) {
                this.flagsCallback = callback;
                callback();
                return () => { this.unsubscribed = true; };
            },
            reloadFeatureFlags() {
                this.reloadCount = (this.reloadCount || 0) + 1;
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
    "({ getAllFeatureFlags() { return undefined; }, get_distinct_id() { return undefined; }, getFeatureFlagResult() { return undefined; } })"
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
private fun readTimestampIso(target: PostHogJsApi): String = js("target.captureOptions.timestamp.toISOString()")
private fun invokeBeforeSend(target: PostHogJsApi): JsAny? = js(
    "target.options.before_send({ event: 'checkout', properties: { distinct_id: 'user-1', " +
        "email: 'person@example.com', plan: 'paid', nullable: null, nested: [['one'], ['two']], " +
        "date: new Date(0) } })"
)
private fun invokeBeforeSendWithoutDistinctId(target: PostHogJsApi): JsAny? =
    js("target.options.before_send({ event: 'checkout', properties: {} })")
private fun invokeBeforeSendWithoutEvent(target: PostHogJsApi): JsAny? =
    js("target.options.before_send({ properties: { distinct_id: 'user-1' } })")
private fun readJsString(target: JsAny, key: String): String = js("target[key]")
private fun readDeepJsString(target: JsAny, parent: String, key: String): String = js("target[parent][key]")
private fun hasDeepJsProperty(target: JsAny, parent: String, key: String): Boolean =
    js("Object.prototype.hasOwnProperty.call(target[parent], key)")
private fun isDeepJsNull(target: JsAny, parent: String, key: String): Boolean = js("target[parent][key] === null")
private fun readDeepJsArrayString(
    target: JsAny,
    parent: String,
    key: String,
    outerIndex: Int,
    innerIndex: Int
): String = js("target[parent][key][outerIndex][innerIndex]")
private fun readDeepJsDateTime(target: JsAny, parent: String, key: String): Double =
    js("target[parent][key].getTime()")
private fun invokeBeforeSendWithThrowingGetter(target: PostHogJsApi): JsAny? =
    js("target.options.before_send(new Proxy({ event: 'checkout' }, " +
        "{ get(target, key) { if (key === 'properties') throw Error('failed'); return target[key]; } }))")
private fun notifyFeatureFlags(target: PostHogJsApi): Unit = js("{ target.flagsCallback(); }")
private fun hasNestedProperty(target: PostHogJsApi, parent: String, key: String): Boolean =
    js("Object.prototype.hasOwnProperty.call(target[parent], key)")
