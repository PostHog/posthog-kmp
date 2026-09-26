@file:Suppress("UnusedParameter")

package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class PostHogJsTest {

    private val calledMethods = mutableListOf<Pair<String, Array<dynamic>>>()
    private var fakeJs: dynamic = null

    @BeforeTest
    fun setup() {
        calledMethods.clear()
        currentConfig = null

        fakeJs = js("{}")

        setupCoreMethods(fakeJs)
        setupFeatureFlagMethods(fakeJs)
        setupMiscMethods(fakeJs)

        mockPostHogJs = fakeJs
    }

    @AfterTest
    fun tearDown() {
        mockPostHogJs = null
        currentConfig = null
    }

    private fun setupCoreMethods(fakeJs: dynamic) {
        fakeJs.capture = { event: String, properties: dynamic, options: dynamic ->
            calledMethods.add("capture" to arrayOf<dynamic>(event, properties, options))
        }
        fakeJs.identify = { distinctId: String, userProperties: dynamic, userPropertiesSetOnce: dynamic ->
            calledMethods.add("identify" to arrayOf<dynamic>(distinctId, userProperties, userPropertiesSetOnce))
        }
        fakeJs.alias = { alias: String ->
            calledMethods.add("alias" to arrayOf<dynamic>(alias))
        }
        fakeJs.reset = { resetDeviceId: dynamic ->
            calledMethods.add("reset" to arrayOf<dynamic>(resetDeviceId))
        }
        fakeJs.get_distinct_id = {
            calledMethods.add("get_distinct_id" to arrayOf<dynamic>())
            "test_distinct_id"
        }
        fakeJs.register = { props: dynamic ->
            calledMethods.add("register" to arrayOf<dynamic>(props))
        }
        fakeJs.unregister = { key: String ->
            calledMethods.add("unregister" to arrayOf<dynamic>(key))
        }
        fakeJs.group = { type: String, key: String, groupProperties: dynamic ->
            calledMethods.add("group" to arrayOf<dynamic>(type, key, groupProperties))
        }
    }

    private fun setupFeatureFlagMethods(fakeJs: dynamic) {
        fakeJs.isFeatureEnabled = { key: String, options: dynamic ->
            calledMethods.add("isFeatureEnabled" to arrayOf<dynamic>(key, options))
            true
        }
        fakeJs.getFeatureFlag = { key: String, options: dynamic ->
            calledMethods.add("getFeatureFlag" to arrayOf<dynamic>(key, options))
            "variant-a"
        }
        fakeJs.getAllFeatureFlags = {
            calledMethods.add("getAllFeatureFlags" to arrayOf<dynamic>())
            null
        }
        fakeJs.reloadFeatureFlags = {
            calledMethods.add("reloadFeatureFlags" to arrayOf<dynamic>())
        }
        fakeJs.onFeatureFlags = { callback: dynamic ->
            calledMethods.add("onFeatureFlags" to arrayOf<dynamic>(callback))
            val unSub: () -> Unit = { }
            unSub
        }
        fakeJs.getFeatureFlagResult = { key: String, options: dynamic ->
            calledMethods.add("getFeatureFlagResult" to arrayOf<dynamic>(key, options))
            null
        }
    }

    private fun setupMiscMethods(fakeJs: dynamic) {
        fakeJs.captureException = { throwable: dynamic, properties: dynamic ->
            calledMethods.add("captureException" to arrayOf<dynamic>(throwable, properties))
        }
        fakeJs.get_property = { key: String ->
            calledMethods.add("get_property" to arrayOf<dynamic>(key))
            if (key == "\$device_id") "test_anon_id" else null
        }
        fakeJs.get_session_id = {
            calledMethods.add("get_session_id" to arrayOf<dynamic>())
            "test_session_id"
        }
        fakeJs.opt_out_capturing = {
            calledMethods.add("opt_out_capturing" to arrayOf<dynamic>())
        }
        fakeJs.opt_in_capturing = {
            calledMethods.add("opt_in_capturing" to arrayOf<dynamic>())
        }
        fakeJs.has_opted_out_capturing = {
            calledMethods.add("has_opted_out_capturing" to arrayOf<dynamic>())
            false
        }
        val requestQueue = js("{}")
        requestQueue.unload = {
            calledMethods.add("requestQueue.unload" to arrayOf<dynamic>())
        }
        fakeJs._requestQueue = requestQueue
        val retryQueue = js("{}")
        retryQueue.unload = {
            calledMethods.add("retryQueue.unload" to arrayOf<dynamic>())
        }
        fakeJs._retryQueue = retryQueue
        fakeJs.shutdown = {
            calledMethods.add("shutdown" to arrayOf<dynamic>())
        }
        fakeJs.debug = { enabled: Boolean ->
            calledMethods.add("debug" to arrayOf<dynamic>(enabled))
        }
        fakeJs.setPersonProperties = { props: dynamic, propsSetOnce: dynamic ->
            calledMethods.add("setPersonProperties" to arrayOf<dynamic>(props, propsSetOnce))
        }
    }

    private fun getCall(methodName: String): Array<dynamic> {
        val calls = calledMethods.filter { it.first == methodName }
        assertEquals(1, calls.size, "Expected one call to $methodName")
        return calls.single().second
    }

    @Test
    fun testCaptureRoutesCorrectly() {
        PostHog.capture("test_event", mapOf("prop" to "value"))
        val call = getCall("capture")
        assertEquals("test_event", call[0] as String)
        assertEquals("value", call[1]["prop"] as String)
    }

    @Test
    fun testCaptureMergesGroupsIntoProperties() {
        PostHog.capture(
            "test_event",
            mapOf("prop" to "value"),
            CaptureOptions(groups = mapOf("company" to "acme"))
        )
        val call = getCall("capture")
        assertEquals("value", call[1]["prop"] as String)
        assertEquals("acme", call[1]["\$groups"]["company"] as String)
    }

    @Test
    fun testCapturePreservesTimestampAsUtcInstant() {
        PostHog.capture(
            "test_event",
            options = CaptureOptions(timestamp = 1_704_164_645_678)
        )

        val timestamp = getCall("capture")[2]["timestamp"]
        assertEquals("2024-01-02T03:04:05.678Z", timestamp.toISOString() as String)
        assertEquals(1_704_164_645_678.0, timestamp.getTime() as Double)
    }

    @Test
    fun testCaptureDropsNullPropertyValues() {
        PostHog.capture("test_event", mapOf("keep" to 1, "drop" to null))
        val call = getCall("capture")
        assertEquals(1, call[1]["keep"] as Int)
        assertFalse(hasOwnProperty(call[1], "drop"), "null-valued properties must be absent, not null")
    }

    @Test
    fun testIdentifyRoutesCorrectly() {
        PostHog.identify("user_123", mapOf("email" to "test@example.com"))
        val call = getCall("identify")
        assertEquals("user_123", call[0] as String)
        assertEquals("test@example.com", call[1]["email"] as String)
    }

    @Test
    fun testScreenRoutesCorrectly() {
        PostHog.screen("Home", mapOf("tab" to "feed"))
        val call = getCall("capture")
        assertEquals("\$screen", call[0] as String)
        assertEquals("Home", call[1]["\$screen_name"] as String)
        assertEquals("feed", call[1]["tab"] as String)
    }
    
    @Test
    fun testSessionIdRoutesCorrectly() {
        assertEquals("test_session_id", PostHog.getSessionId())
    }

    @Test
    fun testDistinctIdRoutesCorrectly() {
        assertEquals("test_distinct_id", PostHog.getDistinctId())
    }

    @Test
    fun testAliasRoutesCorrectly() {
        PostHog.alias("new_alias")
        val call = getCall("alias")
        assertEquals("new_alias", call[0] as String)
    }

    @Test
    fun testResetDoesNotRotateDeviceId() {
        PostHog.reset()
        val call = getCall("reset")
        assertNull(call[0], "reset() must not pass reset_device_id=true")
    }

    @Test
    fun testRegisterRoutesCorrectly() {
        PostHog.register("super_prop", "value")
        val call = getCall("register")
        assertEquals("value", call[0]["super_prop"] as String)
    }

    @Test
    fun testUnregisterRoutesCorrectly() {
        PostHog.unregister("super_prop")
        val call = getCall("unregister")
        assertEquals("super_prop", call[0] as String)
    }

    @Test
    fun testGroupRoutesCorrectly() {
        PostHog.group("company", "posthog", mapOf("plan" to "premium"))
        val call = getCall("group")
        assertEquals("company", call[0] as String)
        assertEquals("posthog", call[1] as String)
        assertEquals("premium", call[2]["plan"] as String)
    }

    @Test
    fun testIsFeatureEnabledRoutesCorrectly() {
        assertTrue(PostHog.isFeatureEnabled("test_flag", defaultValue = false))
        val call = getCall("isFeatureEnabled")
        assertEquals("test_flag", call[0] as String)
        assertEquals(true, call[1]["send_event"] as Boolean)
    }

    @Test
    fun testGetFeatureFlagRoutesCorrectly() {
        assertEquals("variant-a", PostHog.getFeatureFlag("test_flag"))
        val call = getCall("getFeatureFlag")
        assertEquals("test_flag", call[0] as String)
        assertEquals(true, call[1]["send_event"] as Boolean)
    }

    @Test
    fun testReloadFeatureFlagsRoutesCorrectly() {
        PostHog.reloadFeatureFlags()
        getCall("reloadFeatureFlags")
    }

    @Test
    fun testReloadFeatureFlagsCallbackIgnoresStaleImmediateFire() {
        var handler: (() -> Unit)? = null
        fakeJs.onFeatureFlags = { cb: () -> Unit ->
            handler = cb
            cb()
            val unSub: () -> Unit = { handler = null }
            unSub
        }

        var callbackCount = 0
        PostHog.reloadFeatureFlags { callbackCount++ }
        getCall("reloadFeatureFlags")
        assertEquals(0, callbackCount, "callback must not fire with pre-reload flags")

        val registeredHandler = assertNotNull(handler)
        registeredHandler()
        assertEquals(1, callbackCount)
        assertNull(handler, "reload must unsubscribe")

        registeredHandler()
        assertEquals(1, callbackCount, "an already queued notification must not fire the callback twice")
    }

    @Test
    fun testSendFeatureFlagEventFallsBackToConfig() {
        currentConfig = PostHogConfig(apiKey = "key", sendFeatureFlagEvent = false)

        PostHog.getFeatureFlag("test_flag")
        val call = getCall("getFeatureFlag")
        assertEquals(false, call[1]["send_event"] as Boolean)

        PostHog.getFeatureFlag("test_flag", sendFeatureFlagEvent = true)
        val overrideCall = calledMethods.last { it.first == "getFeatureFlag" }.second
        assertEquals(true, overrideCall[1]["send_event"] as Boolean)
    }

    @Test
    fun testSetupMapsPreloadFlagsToFirstLoadOption() {
        fakeJs.init = { apiKey: String, options: dynamic ->
            calledMethods.add("init" to arrayOf<dynamic>(apiKey, options))
        }
        PostHog.setup(PostHogConfig(apiKey = "key", preloadFeatureFlags = false), PostHogContext())

        val call = getCall("init")
        assertEquals("key", call[0] as String)
        assertEquals(true, call[1]["advanced_disable_feature_flags_on_first_load"] as Boolean)
        assertNull(call[1]["advanced_disable_feature_flags"], "flag evaluation must not be disabled permanently")
        assertNull(call[1]["capture_exceptions"], "error tracking must remain unset unless configured")
    }

    @Test
    fun testSetupMapsErrorTrackingToPostHogJs() {
        fakeJs.init = { apiKey: String, options: dynamic ->
            calledMethods.add("init" to arrayOf<dynamic>(apiKey, options))
        }
        PostHog.setup(
            PostHogConfig(apiKey = "key", errorTracking = ErrorTrackingConfig(autoCapture = true)),
            PostHogContext()
        )

        assertEquals(true, getCall("init")[1]["capture_exceptions"] as Boolean)
    }

    @Test
    fun testSetupMapsBeforeSendToPostHogJs() {
        fakeJs.init = { apiKey: String, options: dynamic ->
            calledMethods.add("init" to arrayOf<dynamic>(apiKey, options))
        }
        PostHog.setup(
            PostHogConfig(
                apiKey = "key",
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
        val callback = getCall("init")[1]["before_send"]
        val captureResult = js(
            "({ event: 'checkout', properties: { distinct_id: 'user-1', email: 'person@example.com', " +
                "plan: 'paid', nullable: null, nested: [['one'], ['two']], date: new Date(0) } })"
        )

        val result = callback(captureResult)

        assertEquals("sanitized", result.event as String)
        assertEquals("anonymous", result.properties.distinct_id as String)
        assertNull(result.properties.email)
        assertEquals("paid", result.properties.plan as String)
        assertTrue(hasOwnProperty(result.properties, "nullable"))
        assertNull(result.properties.nullable)
        assertEquals("two", readNestedArrayString(result.properties, "nested", 1, 0))
        assertEquals(0.0, readDateTime(result.properties, "date"))
        assertNull(callback(js("({ event: 'checkout', properties: {} })")))
        assertNull(callback(js("({ properties: { distinct_id: 'user-1' } })")))
    }

    @Test
    fun testBeforeSendContainsCallbackExceptions() {
        var sentinelCalled = false
        fakeJs.init = { _: String, options: dynamic ->
            calledMethods.add("init" to arrayOf<dynamic>(options))
        }
        PostHog.setup(
            PostHogConfig(
                apiKey = "key",
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
        val callback = getCall("init")[0]["before_send"]

        val result = callback(js("({ event: 'checkout', properties: { distinct_id: 'user-1' } })"))

        assertNull(result)
        assertEquals(false, sentinelCalled)
    }

    @Test
    fun testBeforeSendContainsPropertyConversionExceptions() {
        var callbackCalled = false
        fakeJs.init = { _: String, options: dynamic ->
            calledMethods.add("init" to arrayOf<dynamic>(options))
        }
        PostHog.setup(
            PostHogConfig(apiKey = "key", beforeSend = listOf(PostHogBeforeSend {
                callbackCalled = true
                it
            })),
            PostHogContext()
        )
        val callback = getCall("init")[0]["before_send"]
        assertNull(callback(createThrowingCaptureResult()))
        assertFalse(callbackCalled)
    }

    @Test
    fun testGetFeatureFlagResultRoutesCorrectly() {
        assertNull(PostHog.getFeatureFlagResult("test_flag"))
        val call = getCall("getFeatureFlagResult")
        assertEquals("test_flag", call[0] as String)
        assertEquals(true, call[1]["send_event"] as Boolean)
    }

    @Test
    fun testFeatureFlagResultPreservesFields() {
        fakeJs.getFeatureFlagResult = { _: String, _: dynamic ->
            js("({ key: 'checkout', enabled: true, variant: 'blue', payload: 'payload' })")
        }
        assertEquals(
            FeatureFlagResult("checkout", true, "blue", "payload"),
            PostHog.getFeatureFlagResult("checkout")
        )
        fakeJs.getFeatureFlagResult = { _: String, _: dynamic -> js("({ key: 'disabled', enabled: false })") }
        val disabled = assertNotNull(PostHog.getFeatureFlagResult("disabled"))
        assertFalse(disabled.enabled)
        assertNull(disabled.variant)
    }

    @Test
    fun testAllFeatureFlagsPreserveResultsAndHandleUnavailableCache() {
        fakeJs.getAllFeatureFlags = {
            js("[{ key: 'checkout', enabled: true, variant: 'blue', payload: 'payload' }, { key: 'disabled', enabled: false }]")
        }
        val flags = PostHog.getAllFeatureFlags()
        assertEquals(setOf("checkout", "disabled"), flags.keys)
        assertEquals(FeatureFlagResult("checkout", true, "blue", "payload"), flags["checkout"])
        assertEquals(false, flags["disabled"]?.enabled)
        assertNull(flags["disabled"]?.variant)
        fakeJs.getAllFeatureFlags = { undefined }
        assertEquals(emptyMap(), PostHog.getAllFeatureFlags())
    }

    @Test
    fun testFeatureEnabledUsesDefaultOnlyWhenUnavailable() {
        fakeJs.isFeatureEnabled = { _: String, _: dynamic -> false }
        assertFalse(PostHog.isFeatureEnabled("disabled", defaultValue = true))
        fakeJs.isFeatureEnabled = { _: String, _: dynamic -> undefined }
        assertTrue(PostHog.isFeatureEnabled("missing", defaultValue = true))
        assertFalse(PostHog.isFeatureEnabled("missing", defaultValue = false))
    }

    @Test
    fun testCaptureExceptionRoutesCorrectly() {
        val throwable = RuntimeException("Test exception")
        PostHog.captureException(throwable, mapOf("context" to "test"))
        val call = getCall("captureException")
        assertEquals(throwable, call[0] as RuntimeException)
        assertEquals("RuntimeException", call[0].name as String)
        assertEquals("test", call[1]["context"] as String)
    }

    @Test
    fun testGetAnonymousIdRoutesCorrectly() {
        assertEquals("test_anon_id", PostHog.getAnonymousId())
        val call = getCall("get_property")
        assertEquals("\$device_id", call[0] as String)
    }

    @Test
    fun testOptOutRoutesCorrectly() {
        PostHog.optOut()
        getCall("opt_out_capturing")
    }

    @Test
    fun testOptInRoutesCorrectly() {
        PostHog.optIn()
        getCall("opt_in_capturing")
    }

    @Test
    fun testIsOptedOutRoutesCorrectly() {
        assertFalse(PostHog.isOptedOut())
        getCall("has_opted_out_capturing")
        fakeJs.has_opted_out_capturing = { true }
        assertTrue(PostHog.isOptedOut())
    }

    @Test
    fun testFlushDrainsRequestQueues() {
        PostHog.flush()
        getCall("requestQueue.unload")
        getCall("retryQueue.unload")
    }

    @Test
    fun testCloseRoutesCorrectly() {
        PostHog.close()
        getCall("shutdown")
    }

    @Test
    fun testSetDebugRoutesCorrectly() {
        PostHog.setDebug(true)
        val call = getCall("debug")
        assertEquals(true, call[0] as Boolean)
    }

    @Test
    fun testSetPersonPropertiesRoutesCorrectly() {
        PostHog.setPersonProperties(mapOf("plan" to "premium"), mapOf("first_login" to true))
        val call = getCall("setPersonProperties")
        assertEquals("premium", call[0]["plan"] as String)
        assertEquals(true, call[1]["first_login"] as Boolean)
    }
}

private fun hasOwnProperty(target: dynamic, key: String): Boolean =
    js("Object.prototype.hasOwnProperty.call(target, key)")
private fun readNestedArrayString(target: dynamic, key: String, outerIndex: Int, innerIndex: Int): String =
    js("target[key][outerIndex][innerIndex]")
private fun readDateTime(target: dynamic, key: String): Double = js("target[key].getTime()")
private fun createThrowingCaptureResult(): dynamic =
    js("new Proxy({ event: 'checkout' }, { get(target, key) { if (key === 'properties') throw new Error('failed'); return target[key]; } })")
