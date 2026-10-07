package com.posthog.kmp

import java.time.OffsetDateTime
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertNull

class PostHogJvmTest {

    private lateinit var fakeInterface: FakePostHogInterface

    @BeforeTest
    fun setup() {
        fakeInterface = FakePostHogInterface()
        postHogInstance = fakeInterface.proxy
        currentConfig = null
    }

    @AfterTest
    fun tearDown() {
        postHogInstance = null
        currentConfig = null
    }

    private fun assertMethodCalled(methodName: String, vararg args: Any?) {
        val calls = fakeInterface.calledMethods.filter { it.first == methodName }
        assertEquals(1, calls.size, "Expected one call to $methodName")
        assertEquals(args.toList(), calls.single().second, "Arguments for $methodName")
    }

    @Test
    fun testConfigureErrorTracking() {
        val nativeConfig = com.posthog.PostHogConfig("key")

        nativeConfig.configureErrorTracking(
            ErrorTrackingConfig(
                autoCapture = true,
                inAppIncludes = listOf("com.example"),
                ignoredExceptionTypes = listOf(IllegalStateException::class)
            )
        )

        assertEquals(true, nativeConfig.errorTrackingConfig.autoCapture)
        assertEquals(listOf("com.example"), nativeConfig.errorTrackingConfig.inAppIncludes)
        assertEquals(
            listOf<Class<out Throwable>>(IllegalStateException::class.java),
            nativeConfig.errorTrackingConfig.ignoredExceptionTypes
        )
    }

    @Test
    fun testCaptureRoutesCorrectly() {
        PostHog.capture("test_event", mapOf("prop" to "value"))
        assertMethodCalled("capture", "test_event", null, mapOf("prop" to "value"), null, null, null, null)
    }

    @Test
    fun testCapturePassesGroupsToNativeParameter() {
        PostHog.capture(
            "test_event",
            mapOf("prop" to "value"),
            CaptureOptions(groups = mapOf("company" to "acme"))
        )
        // capture(event, distinctId, properties, userProperties, userPropertiesSetOnce, groups, timestamp)
        assertMethodCalled(
            "capture",
            "test_event",
            null,
            mapOf("prop" to "value"),
            null,
            null,
            mapOf("company" to "acme"),
            null
        )
    }

    @Test
    fun testCapturePreservesTimestampInstantAcrossDefaultTimeZones() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val timestamp = OffsetDateTime.parse("2024-01-01T17:04:05.678-10:00")
                .toInstant()
                .toEpochMilli()

            PostHog.capture("test_event", options = CaptureOptions(timestamp = timestamp))

            assertMethodCalled("capture", "test_event", null, null, null, null, null, Date(timestamp))
            assertEquals("2024-01-02T03:04:05.678Z", Date(timestamp).toInstant().toString())
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun testCaptureDropsNullPropertyValues() {
        PostHog.capture("test_event", mapOf("keep" to 1, "drop" to null))
        assertMethodCalled("capture", "test_event", null, mapOf("keep" to 1), null, null, null, null)
    }

    @Test
    fun testIdentifyRoutesCorrectly() {
        PostHog.identify("user_123", mapOf("email" to "test@example.com"))
        assertMethodCalled("identify", "user_123", mapOf("email" to "test@example.com"), null)
    }

    @Test
    fun testScreenRoutesCorrectly() {
        PostHog.screen("Home", mapOf("tab" to "feed"))
        assertMethodCalled("screen", "Home", mapOf("tab" to "feed"))
    }

    @Test
    fun testScreenNameWinsOverProperty() {
        PostHog.screen("Home", mapOf("\$screen_name" to "Override", "tab" to "feed"))
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
        assertEquals(false, PostHog.isFeatureEnabled("test_flag", defaultValue = true))
        assertMethodCalled("isFeatureEnabled", "test_flag", true, true)
    }

    @Test
    fun testSendFeatureFlagEventFallsBackToConfig() {
        currentConfig = PostHogConfig(projectToken = "key", sendFeatureFlagEvent = false)

        PostHog.isFeatureEnabled("test_flag")
        assertMethodCalled("isFeatureEnabled", "test_flag", false, false)

        PostHog.isFeatureEnabled("test_flag", sendFeatureFlagEvent = true)
        val overrideCall = fakeInterface.calledMethods.last { it.first == "isFeatureEnabled" }
        assertEquals(true, overrideCall.second[2])
    }

    @Test
    fun testGetFeatureFlagRoutesCorrectly() {
        fakeInterface.featureFlag = "variant-a"
        assertEquals("variant-a", PostHog.getFeatureFlag("test_flag"))
        assertMethodCalled("getFeatureFlag", "test_flag", null, true)
    }

    @Test
    fun testReloadFeatureFlagsRoutesCorrectly() {
        PostHog.reloadFeatureFlags()
        assertMethodCalled("reloadFeatureFlags", null)
    }

    @Test
    fun testGetFeatureFlagResultRoutesCorrectly() {
        fakeInterface.featureFlagResult = com.posthog.FeatureFlagResult("test_flag", true, "blue", mapOf("color" to "blue"))
        assertEquals(
            FeatureFlagResult("test_flag", true, "blue", mapOf("color" to "blue")),
            PostHog.getFeatureFlagResult("test_flag")
        )
        assertMethodCalled("getFeatureFlagResult", "test_flag", true)
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
        fakeInterface.optedOut = true
        assertTrue(PostHog.isOptedOut())
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
        PostHog.capture("after_close")
        PostHog.close()
        assertEquals(listOf("close"), fakeInterface.calledMethods.map { it.first })
        assertNull(PostHog.getDistinctId())
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

    @Test
    fun testAllFeatureFlagsPreserveDisabledAndVariantResults() {
        fakeInterface.featureFlags = listOf(
            com.posthog.FeatureFlagResult("disabled", false, null, null),
            com.posthog.FeatureFlagResult("checkout", true, "blue", listOf("one", "two"))
        )
        assertEquals(
            mapOf(
                "disabled" to FeatureFlagResult("disabled", false),
                "checkout" to FeatureFlagResult("checkout", true, "blue", listOf("one", "two"))
            ),
            PostHog.getAllFeatureFlags()
        )
        assertMethodCalled("getAllFeatureFlags")
    }

    @Test
    fun testUnknownFeatureFlagReturnsNull() {
        assertNull(PostHog.getFeatureFlag("missing"))
        assertNull(PostHog.getFeatureFlagResult("missing"))
        assertEquals(emptyMap(), PostHog.getAllFeatureFlags())
    }

    @Test
    fun testReloadCallbackWaitsForNativeCompletion() {
        var completions = 0
        PostHog.reloadFeatureFlags { completions++ }
        assertEquals(0, completions)
        val callback = fakeInterface.calledMethods.single().second.single() as com.posthog.PostHogOnFeatureFlags
        callback.loaded()
        assertEquals(1, completions)
    }

    @Test
    fun testUninitializedGettersReturnSafeDefaults() {
        postHogInstance = null
        assertNull(PostHog.getDistinctId())
        assertNull(PostHog.getAnonymousId())
        assertNull(PostHog.getSessionId())
        assertNull(PostHog.getFeatureFlag("missing"))
        assertNull(PostHog.getFeatureFlagResult("missing"))
        assertEquals(emptyMap(), PostHog.getAllFeatureFlags())
        assertEquals(false, PostHog.isFeatureEnabled("missing"))
        assertEquals(true, PostHog.isFeatureEnabled("missing", defaultValue = true))
        assertEquals(false, PostHog.isOptedOut())
        assertTrue(fakeInterface.calledMethods.isEmpty())
    }

    @Test
    fun testNullRegistrationDoesNotReachNativeSdk() {
        PostHog.register("ignored", null)
        assertTrue(fakeInterface.calledMethods.isEmpty())
    }

    @Test
    fun testIdentifyForwardsSetOnceAndDropsNulls() {
        PostHog.identify("user", mapOf("drop" to null), mapOf("first" to true, "drop" to null))
        assertMethodCalled("identify", "user", null, mapOf("first" to true))
    }

    @Test
    fun testBeforeSendWithoutCallbacksLeavesNativeHookUnset() {
        val nativeConfig = com.posthog.PostHogConfig("key")
        nativeConfig.configureBeforeSend(PostHogConfig(projectToken = "key"))
        assertTrue(nativeConfig.beforeSendList.isEmpty())
    }

    @Test
    fun testNativeBeforeSendDropsEventsAndContainsCallbackFailures() {
        for (callback in listOf(PostHogBeforeSend { null }, PostHogBeforeSend { error("failed") })) {
            val nativeConfig = com.posthog.PostHogConfig("key")
            nativeConfig.configureBeforeSend(PostHogConfig(projectToken = "key", beforeSend = listOf(callback)))
            assertNull(nativeConfig.beforeSendList.single().run(
                com.posthog.PostHogEvent("secret", "user", mutableMapOf("email" to "private"))
            ))
        }
    }

    @Test
    fun testBeforeSendDelegatesToNativeConfigAndPreservesMetadata() {
        val wrapperConfig = PostHogConfig(
            projectToken = "key",
            beforeSend = listOf(
                PostHogBeforeSend {
                    it.copy(
                        event = "sanitized",
                        distinctId = "anonymous",
                        properties = it.properties - "email"
                    )
                }
            )
        )
        val nativeConfig = com.posthog.PostHogConfig("key")
        nativeConfig.configureBeforeSend(wrapperConfig)
        val timestamp = Date(1234)
        val uuid = UUID.randomUUID()

        val result = nativeConfig.beforeSendList.single().run(
            com.posthog.PostHogEvent(
                event = "checkout",
                distinctId = "user-1",
                properties = mutableMapOf("email" to "person@example.com", "plan" to "paid"),
                timestamp = timestamp,
                uuid = uuid
            )
        )

        assertEquals("sanitized", result?.event)
        assertEquals("anonymous", result?.distinctId)
        assertEquals("paid", result?.properties?.get("plan"))
        assertEquals(false, result?.properties?.containsKey("email"))
        assertEquals(timestamp, result?.timestamp)
        assertEquals(uuid, result?.uuid)
    }
}
