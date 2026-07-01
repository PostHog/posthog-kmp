package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

class PostHogJsTest {

    private val calledMethods = mutableListOf<Pair<String, Array<dynamic>>>()

    @BeforeTest
    fun setup() {
        calledMethods.clear()

        val fakeJs = js("{}")
        
        setupCoreMethods(fakeJs)
        setupFeatureFlagMethods(fakeJs)
        setupMiscMethods(fakeJs)

        mockPostHogJs = fakeJs
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
        fakeJs.reset = { clear: Boolean ->
            calledMethods.add("reset" to arrayOf<dynamic>(clear))
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
        fakeJs.flush = {
            calledMethods.add("flush" to arrayOf<dynamic>())
        }
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
        val call = calledMethods.find { it.first == methodName }
        assertTrue(call != null, "Method $methodName was not called")
        return call.second
    }

    @Test
    fun testCaptureRoutesCorrectly() {
        PostHog.capture("test_event", mapOf("prop" to "value"))
        val call = getCall("capture")
        assertEquals("test_event", call[0] as String)
        assertEquals("value", call[1]["prop"] as String)
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
    fun testResetRoutesCorrectly() {
        PostHog.reset()
        val call = getCall("reset")
        assertEquals(true, call[0] as Boolean)
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
        PostHog.isFeatureEnabled("test_flag", defaultValue = true)
        val call = getCall("isFeatureEnabled")
        assertEquals("test_flag", call[0] as String)
        assertEquals(true, call[1]["send_event"] as Boolean)
    }

    @Test
    fun testGetFeatureFlagRoutesCorrectly() {
        PostHog.getFeatureFlag("test_flag")
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
    fun testGetFeatureFlagResultRoutesCorrectly() {
        PostHog.getFeatureFlagResult("test_flag")
        val call = getCall("getFeatureFlagResult")
        assertEquals("test_flag", call[0] as String)
        assertEquals(true, call[1]["send_event"] as Boolean)
    }

    @Test
    fun testCaptureExceptionRoutesCorrectly() {
        val throwable = RuntimeException("Test exception")
        PostHog.captureException(throwable, mapOf("context" to "test"))
        val call = getCall("captureException")
        assertEquals(throwable, call[0] as RuntimeException)
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
        PostHog.isOptedOut()
        getCall("has_opted_out_capturing")
    }

    @Test
    fun testFlushRoutesCorrectly() {
        PostHog.flush()
        getCall("flush")
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
