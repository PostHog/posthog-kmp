package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PostHogConfigTest {

    @Test
    fun defaultsToUsCloudHost() {
        assertEquals(PostHogConfig.HOST_US, PostHogConfig("phc_test").host)
    }

    @Test
    fun captureScreenViewsDefaultsToEnabled() {
        assertEquals(true, PostHogConfig("phc_test").captureScreenViews)
    }

    @Test
    fun defaultsToIdentifiedOnlyPersonProfiles() {
        assertEquals(PersonProfiles.IDENTIFIED_ONLY, PostHogConfig("phc_test").personProfiles)
    }

    @Test
    fun pushNotificationCaptureDefaultsToEnabled() {
        val config = PostHogConfig("phc_test")

        assertEquals(true, config.capturePushNotificationSubscriptions)
        assertEquals(true, config.capturePushNotificationOpened)
    }

    @Test
    fun surveysDefaultsToEnabled() {
        assertEquals(true, PostHogConfig("phc_test").surveys)
    }

    @Test
    fun defaultsErrorTrackingToNull() {
        assertNull(PostHogConfig("phc_test").errorTracking)
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
    fun sessionReplayDefaultsMatchNativeSdks() {
        val config = SessionReplayConfig()
        assertEquals(true, config.maskAllTextInputs)
        assertEquals(true, config.maskAllImages)
        assertEquals(false, config.captureLogs)
        assertEquals(true, config.captureLogcat)
        assertEquals(1000L, config.throttleDelayMs)
        assertEquals(false, config.verifyScreenshotMaskAlignment)
        assertEquals(IosCaptureMethod.STANDARD, config.iosCaptureMethod)
    }

    @Test
    fun beforeSendWithoutCallbacksPreservesEvent() {
        val event = PostHogEvent("checkout", "user-1", mapOf("plan" to "paid"))
        val config = PostHogConfig("key")
        assertSame(event, config.runBeforeSend(event))
    }

    @Test
    fun beforeSendCallbacksRunInOrder() {
        val config = PostHogConfig("phc_test") {
            beforeSend = listOf(
                PostHogBeforeSend { it.copy { properties = it.properties + ("first" to true) } },
                PostHogBeforeSend {
                    it.copy { properties = it.properties + ("saw_first" to it.properties.containsKey("first")) }
                }
            )
        }

        val result = config.runBeforeSend(PostHogEvent("checkout", "user-1", emptyMap()))

        assertEquals(mapOf("first" to true, "saw_first" to true), result?.properties)
    }

    @Test
    fun beforeSendKeepsOptionsSetByEarlierCallback() {
        val config = PostHogConfig("phc_test") {
            beforeSend = listOf(
                PostHogBeforeSend { it.copy { options = mapOf("process_person_profile" to false) } },
                PostHogBeforeSend { PostHogEvent(it.event, it.distinctId, it.properties) }
            )
        }

        val result = config.runBeforeSend(
            PostHogEvent("checkout", "user-1", emptyMap(), mapOf("process_person_profile" to true))
        )

        assertEquals(mapOf<String, Any?>("process_person_profile" to false), result?.options)
    }

    @Test
    fun beforeSendFailureMessageNamesEventAndExceptionTypeOnly() {
        val message = beforeSendFailureMessage(
            PostHogEvent("checkout", "user-1", emptyMap()),
            IllegalStateException("secret@example.com")
        )

        assertEquals("Before-send callback threw IllegalStateException; event \"checkout\" was dropped.", message)
    }

    @Test
    fun beforeSendDropsTransformedEventAndStopsAfterCallbackException() {
        var errorCount = 0
        var sentinelCalled = false
        val failure = IllegalStateException("failed")
        val config = PostHogConfig("phc_test") {
            beforeSend = listOf(
                PostHogBeforeSend { it.copy { this.event = "transformed" } },
                PostHogBeforeSend { throw failure },
                PostHogBeforeSend {
                    sentinelCalled = true
                    it
                }
            )
        }

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
        val config = PostHogConfig("phc_test") {
            beforeSend = listOf(
                PostHogBeforeSend { null },
                PostHogBeforeSend {
                    finalCallbackCalled = true
                    it
                }
            )
        }

        assertNull(config.runBeforeSend(PostHogEvent("checkout", "user-1", emptyMap())))
        assertEquals(false, finalCallbackCalled)
    }
}
