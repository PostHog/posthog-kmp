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
}
