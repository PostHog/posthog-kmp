package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostHogConfigTest {

    @Test
    fun defaultsToUsCloudHost() {
        assertEquals(PostHogConfig.HOST_US, PostHogConfig(apiKey = "phc_test").host)
    }

    @Test
    fun defaultsToIdentifiedOnlyPersonProfiles() {
        assertEquals(PersonProfiles.IDENTIFIED_ONLY, PostHogConfig(apiKey = "phc_test").personProfiles)
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
