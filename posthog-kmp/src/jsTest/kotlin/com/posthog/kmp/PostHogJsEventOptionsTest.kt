package com.posthog.kmp

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostHogJsEventOptionsTest {

    private val calls = mutableListOf<Pair<String, dynamic>>()

    @BeforeTest
    fun setup() {
        val fakeJs: dynamic = js("{}")
        fakeJs.capture = { _: String, properties: dynamic, _: dynamic -> calls.add("capture" to properties) }
        fakeJs.register = { properties: dynamic -> calls.add("register" to properties) }
        fakeJs.unregister = { key: String -> calls.add("unregister" to key) }
        mockPostHogJs = fakeJs
        currentConfig = null
    }

    @AfterTest
    fun tearDown() {
        mockPostHogJs = null
    }

    private fun argumentsOf(method: String): List<dynamic> = calls.filter { it.first == method }.map { it.second }

    @Test
    fun testEventOptionsAreSentAsLegacyPropertiesThatWin() {
        val options = PostHogEventOptions(cookielessMode = true, disableSkewCorrection = true, productTourId = "tour")
        PostHog.capture(
            "test_event",
            mapOf("\$cookieless_mode" to false, "prop" to "value"),
            CaptureOptions(eventOptions = options)
        )
        PostHog.screen("Home", eventOptions = options)

        val (capture, screen) = argumentsOf("capture")
        for (properties in listOf(capture, screen)) {
            assertEquals(true, properties["\$cookieless_mode"] as Boolean)
            assertEquals(true, properties["\$ignore_sent_at"] as Boolean)
            assertEquals("tour", properties["\$product_tour_id"] as String)
        }
        assertEquals("value", capture["prop"] as String)
        assertEquals("Home", screen["\$screen_name"] as String)
    }

    @Test
    fun testCaptureAiSendsNothingAndReturnsNull() {
        assertNull(PostHog.captureAi("\$ai_generation", mapOf("\$ai_model" to "gpt-5")))
        assertTrue(calls.isEmpty())
    }

    @Test
    fun testRegisterOptionsRegistersLegacyProperties() {
        PostHog.registerOptions(PostHogEventOptions(cookielessMode = true))
        PostHog.unregisterOptions("cookieless_mode")
        PostHog.unregisterOptions("unknown")

        assertEquals(true, argumentsOf("register").single()["\$cookieless_mode"] as Boolean)
        assertEquals(listOf<dynamic>("\$cookieless_mode"), argumentsOf("unregister"))
    }
}
