package com.posthog.kmp

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneForSecondsFromGMT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostHogAppleTest {

    @Test
    fun testNativeAnalyticsThroughPublicApi() {
        val events = mutableListOf<PostHogEvent>()
        PostHog.setup(
            PostHogConfig(
                projectToken = "apple-unit-test",
                host = "http://127.0.0.1:9",
                preloadFeatureFlags = false,
                captureApplicationLifecycleEvents = false,
                beforeSend = listOf(PostHogBeforeSend { event ->
                    events.add(event)
                    null
                }),
            ),
            PostHogContext(),
        )
        try {
            PostHog.reset()
            assertNotNull(PostHog.getDistinctId())
            PostHog.identify("apple-test-user")
            assertEquals("apple-test-user", PostHog.getDistinctId())
            PostHog.register("theme", "dark")
            PostHog.capture("apple-test-event", mapOf("enabled" to true))
            val event = events.single { it.event == "apple-test-event" }
            assertEquals("apple-test-user", event.distinctId)
            assertEquals("dark", event.properties["theme"])
            assertEquals("posthog-kmp", event.properties["\$lib"])

            PostHog.optOut()
            assertTrue(PostHog.isOptedOut())
            PostHog.capture("opted-out-event")
            assertTrue(events.none { it.event == "opted-out-event" })
            PostHog.optIn()
            assertFalse(PostHog.isOptedOut())
        } finally {
            PostHog.reset()
            PostHog.close()
        }
    }

    @Test
    fun testScreenNameWinsOverProperty() {
        val events = mutableListOf<PostHogEvent>()
        PostHog.setup(
            PostHogConfig(
                apiKey = "apple-unit-test",
                host = "http://127.0.0.1:9",
                preloadFeatureFlags = false,
                captureApplicationLifecycleEvents = false,
                beforeSend = listOf(PostHogBeforeSend { event ->
                    events.add(event)
                    null
                }),
            ),
            PostHogContext(),
        )
        try {
            PostHog.screen("Home", mapOf("\$screen_name" to "Override", "tab" to "feed"))
            val event = events.single { it.event == "\$screen" }
            assertEquals("Home", event.properties["\$screen_name"])
            assertEquals("feed", event.properties["tab"])
        } finally {
            PostHog.reset()
            PostHog.close()
        }
    }

    @Test
    fun testTimestampConversionPreservesUtcInstant() {
        val timestamp = 1_704_164_645_678L

        val date = timestamp.toNSDate()

        val formatter = NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        }
        assertEquals("2024-01-02T03:04:05.678Z", formatter.stringFromDate(date))
    }
}
