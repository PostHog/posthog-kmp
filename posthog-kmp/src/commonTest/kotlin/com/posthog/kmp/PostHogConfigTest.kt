package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PostHogConfigTest {

    private class IgnoredException : RuntimeException()

    @Test
    fun defaultsToUsCloudHost() {
        assertEquals(PostHogConfig.HOST_US, PostHogConfig(apiKey = "phc_test").host)
    }

    @Test
    fun defaultsToIdentifiedOnlyPersonProfiles() {
        assertEquals(PersonProfiles.IDENTIFIED_ONLY, PostHogConfig(apiKey = "phc_test").personProfiles)
    }

    @Test
    fun defaultsErrorTrackingToNull() {
        assertNull(PostHogConfig(apiKey = "phc_test").errorTracking)
    }

    @Test
    fun errorTrackingDefaultsMatchNativeSdks() {
        val config = ErrorTrackingConfig()
        assertEquals(false, config.autoCapture)
        assertEquals(emptyList(), config.inAppIncludes)
        assertEquals(emptyList(), config.ignoredExceptionTypes)
        assertEquals(emptyList(), config.inAppExcludes)
        assertEquals(true, config.inAppByDefault)
    }

    @Test
    fun copyPreservesErrorTracking() {
        val errorTracking = ErrorTrackingConfig(
            autoCapture = true,
            inAppIncludes = listOf("com.example"),
            ignoredExceptionTypes = listOf(IgnoredException::class),
            inAppExcludes = listOf("ThirdParty"),
            inAppByDefault = false
        )
        val config = PostHogConfig(apiKey = "phc_test", errorTracking = errorTracking)

        assertEquals(errorTracking, config.copy(debug = true).errorTracking)
    }

    @Test
    fun sessionRecordingDefaultsMatchNativeSdks() {
        val config = SessionRecordingConfig()
        assertEquals(true, config.maskAllTextInputs)
        assertEquals(true, config.maskAllImages)
        assertEquals(false, config.captureLogs)
        assertEquals(true, config.captureLogcat)
        assertEquals(1000L, config.debouncerDelayMs)
    }

    @Test
    fun beforeSendWithoutCallbacksPreservesEvent() {
        val event = PostHogEvent("checkout", "user-1", mapOf("plan" to "paid"))
        val config = PostHogConfig(apiKey = "key")
        assertSame(event, config.runBeforeSend(event))
    }

    @Test
    fun beforeSendCallbacksRunInOrder() {
        val config = PostHogConfig(
            apiKey = "phc_test",
            beforeSend = listOf(
                PostHogBeforeSend { it.copy(properties = it.properties + ("first" to true)) },
                PostHogBeforeSend {
                    it.copy(properties = it.properties + ("saw_first" to it.properties.containsKey("first")))
                }
            )
        )

        val result = config.runBeforeSend(PostHogEvent("checkout", "user-1", emptyMap()))

        assertEquals(mapOf("first" to true, "saw_first" to true), result?.properties)
    }

    @Test
    fun copyPreservesBeforeSendCallbacks() {
        val callback = PostHogBeforeSend { it }
        val config = PostHogConfig(apiKey = "phc_test", beforeSend = listOf(callback))

        val copy = config.copy(debug = true)

        assertEquals(listOf(callback), copy.beforeSend)
    }

    @Test
    fun beforeSendDropsTransformedEventAndStopsAfterCallbackException() {
        var errorCount = 0
        var sentinelCalled = false
        val failure = IllegalStateException("failed")
        val config = PostHogConfig(
            apiKey = "phc_test",
            beforeSend = listOf(
                PostHogBeforeSend { it.copy(event = "transformed") },
                PostHogBeforeSend { throw failure },
                PostHogBeforeSend {
                    sentinelCalled = true
                    it
                }
            )
        )

        val result = config.runBeforeSend(PostHogEvent("checkout", "user-1", emptyMap())) {
            assertSame(failure, it)
            errorCount++
        }

        assertNull(result)
        assertEquals(false, sentinelCalled)
        assertEquals(1, errorCount)
    }

    @Test
    fun beforeSendStopsAfterDroppedEvent() {
        var finalCallbackCalled = false
        val config = PostHogConfig(
            apiKey = "phc_test",
            beforeSend = listOf(
                PostHogBeforeSend { null },
                PostHogBeforeSend {
                    finalCallbackCalled = true
                    it
                }
            )
        )

        assertNull(config.runBeforeSend(PostHogEvent("checkout", "user-1", emptyMap())))
        assertEquals(false, finalCallbackCalled)
    }
}
