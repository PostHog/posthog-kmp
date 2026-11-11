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
            assertEquals(PostHogKmpVersion.VERSION, event.properties["\$lib_version"])

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
    fun testEventOptionsReachBeforeSend() {
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
            PostHog.capture("anonymous")
            PostHog.registerOptions(PostHogEventOptions(cookielessMode = true))
            PostHog.capture(
                "with-options",
                options = CaptureOptions(eventOptions = PostHogEventOptions(processPersonProfile = true, productTourId = "tour"))
            )
            PostHog.unregisterOptions("cookieless_mode")
            PostHog.capture("unregistered")
            val aiUuid = PostHog.captureAi("\$ai_generation", options = CaptureOptions(eventOptions = PostHogEventOptions(disableSkewCorrection = true)))

            // The SDK marks an anonymous user's event as personless with the default personProfiles
            assertEquals(PostHogEventOptions(processPersonProfile = false), events.single { it.event == "anonymous" }.options)
            assertEquals(
                PostHogEventOptions(processPersonProfile = true, cookielessMode = true, productTourId = "tour"),
                events.single { it.event == "with-options" }.options
            )
            assertEquals(null, events.single { it.event == "unregistered" }.options?.cookielessMode)
            assertEquals(true, events.single { it.event == "\$ai_generation" }.options?.disableSkewCorrection)
            assertEquals(null, aiUuid, "beforeSend dropped the AI event")
        } finally {
            PostHog.reset()
            PostHog.close()
        }
    }

    @Test
    fun testAppLanguageOverridesDetectionAndYieldsToRegistered() {
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
            PostHog.setAppLanguage("pt_BR")
            PostHog.reset()
            PostHog.capture("overridden")
            PostHog.register("\$app_language", "de")
            PostHog.capture("registered")

            assertEquals("pt-BR", events.single { it.event == "overridden" }.properties["\$app_language"])
            assertEquals("de", events.single { it.event == "registered" }.properties["\$app_language"])
        } finally {
            PostHog.setAppLanguage(null)
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
    fun testFeatureFlagsWithoutLoadedFlags() {
        PostHog.setup(
            PostHogConfig(
                projectToken = "apple-unit-test",
                host = "http://127.0.0.1:9",
                preloadFeatureFlags = false,
                captureApplicationLifecycleEvents = false,
            ),
            PostHogContext(),
        )
        try {
            assertTrue(PostHog.isFeatureEnabled("missing", defaultValue = true, sendFeatureFlagEvent = false))
            assertFalse(PostHog.isFeatureEnabled("missing", sendFeatureFlagEvent = false))
        } finally {
            PostHog.close()
        }

        val results = mutableListOf<PostHogFeatureFlagsLoaded>()
        PostHog.reloadFeatureFlags { loaded ->
            results.add(loaded)
            error("callback failure must not reach the native SDK")
        }
        assertTrue(results.single().errorsLoading)
        assertTrue(results.single().flags.isEmpty())
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
