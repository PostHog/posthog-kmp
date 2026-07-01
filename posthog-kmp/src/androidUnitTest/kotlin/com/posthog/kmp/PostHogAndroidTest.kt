package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

class PostHogAndroidTest {

    private lateinit var fakeInterface: FakePostHogInterface

    @BeforeTest
    fun setup() {
        fakeInterface = FakePostHogInterface()
        postHogInstance = fakeInterface.proxy
    }

    private fun assertMethodCalled(methodName: String, vararg args: Any?) {
        val call = fakeInterface.calledMethods.find { it.first == methodName }
        assertTrue(call != null, "Method $methodName was not called")
        
        args.forEachIndexed { index, expectedArg ->
            if (expectedArg != null) {
                assertEquals(expectedArg, call.second.getOrNull(index), "Argument at index $index mismatch for $methodName")
            }
        }
    }

    @Test
    fun testCaptureRoutesCorrectly() {
        PostHog.capture("test_event", mapOf("prop" to "value"))
        assertMethodCalled("capture", "test_event", null, mapOf("prop" to "value"))
    }

    @Test
    fun testIdentifyRoutesCorrectly() {
        PostHog.identify("user_123", mapOf("email" to "test@example.com"))
        assertMethodCalled("identify", "user_123", mapOf("email" to "test@example.com"))
    }

    @Test
    fun testScreenRoutesCorrectly() {
        PostHog.screen("Home", mapOf("tab" to "feed"))
        assertMethodCalled("screen", "Home", mapOf("tab" to "feed"))
    }
    
    @Test
    fun testSessionIdRoutesCorrectly() {
        fakeInterface.currentSessionId = "00000000-0000-0000-0000-000000000123"
        assertEquals("00000000-0000-0000-0000-000000000123", PostHog.getSessionId())
    }

    @Test
    fun testDistinctIdRoutesCorrectly() {
        fakeInterface.currentDistinctId = "distinct_123"
        assertEquals("distinct_123", PostHog.getDistinctId())
    }

    @Test
    fun testAliasRoutesCorrectly() {
        PostHog.alias("new_alias")
        assertMethodCalled("alias", "new_alias")
    }

    @Test
    fun testResetRoutesCorrectly() {
        PostHog.reset()
        assertMethodCalled("reset")
    }

    @Test
    fun testRegisterRoutesCorrectly() {
        PostHog.register("super_prop", "value")
        assertMethodCalled("register", "super_prop", "value")
    }

    @Test
    fun testUnregisterRoutesCorrectly() {
        PostHog.unregister("super_prop")
        assertMethodCalled("unregister", "super_prop")
    }

    @Test
    fun testGroupRoutesCorrectly() {
        PostHog.group("company", "posthog", mapOf("plan" to "premium"))
        assertMethodCalled("group", "company", "posthog", mapOf("plan" to "premium"))
    }

    @Test
    fun testIsFeatureEnabledRoutesCorrectly() {
        PostHog.isFeatureEnabled("test_flag", defaultValue = true)
        assertMethodCalled("isFeatureEnabled", "test_flag", true)
    }

    @Test
    fun testGetFeatureFlagRoutesCorrectly() {
        PostHog.getFeatureFlag("test_flag")
        assertMethodCalled("getFeatureFlag", "test_flag")
    }

    @Test
    fun testReloadFeatureFlagsRoutesCorrectly() {
        PostHog.reloadFeatureFlags()
        assertMethodCalled("reloadFeatureFlags")
    }

    @Test
    fun testGetFeatureFlagResultRoutesCorrectly() {
        PostHog.getFeatureFlagResult("test_flag")
        assertMethodCalled("getFeatureFlagResult", "test_flag")
    }

    @Test
    fun testCaptureExceptionRoutesCorrectly() {
        val throwable = RuntimeException("Test exception")
        PostHog.captureException(throwable, mapOf("context" to "test"))
        assertMethodCalled("captureException", throwable, mapOf("context" to "test"))
    }

    @Test
    fun testGetAnonymousIdRoutesCorrectly() {
        fakeInterface.currentAnonymousId = "anon-id-123"
        assertEquals("anon-id-123", PostHog.getAnonymousId())
        assertMethodCalled("getAnonymousId")
    }

    @Test
    fun testOptOutRoutesCorrectly() {
        PostHog.optOut()
        assertMethodCalled("optOut")
    }

    @Test
    fun testOptInRoutesCorrectly() {
        PostHog.optIn()
        assertMethodCalled("optIn")
    }

    @Test
    fun testIsOptedOutRoutesCorrectly() {
        PostHog.isOptedOut()
        assertMethodCalled("isOptOut")
    }

    @Test
    fun testFlushRoutesCorrectly() {
        PostHog.flush()
        assertMethodCalled("flush")
    }

    @Test
    fun testCloseRoutesCorrectly() {
        PostHog.close()
        assertMethodCalled("close")
    }

    @Test
    fun testSetDebugRoutesCorrectly() {
        PostHog.setDebug(true)
        assertMethodCalled("debug", true)
    }

    @Test
    fun testSetPersonPropertiesRoutesCorrectly() {
        PostHog.setPersonProperties(mapOf("plan" to "premium"), mapOf("first_login" to true))
        assertMethodCalled("setPersonProperties", mapOf("plan" to "premium"), mapOf("first_login" to true))
    }
}
