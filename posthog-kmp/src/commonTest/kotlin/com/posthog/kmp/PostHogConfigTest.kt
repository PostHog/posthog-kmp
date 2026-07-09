package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
