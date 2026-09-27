@file:OptIn(ExperimentalWasmJsInterop::class)

package com.posthog.kmp

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostHogWasmFeatureFlagTest {
    @BeforeTest
    fun setUp() {
        mockPostHogWasmJs = createFeatureFlagFake()
    }

    @AfterTest
    fun tearDown() {
        mockPostHogWasmJs = null
    }

    @Test
    fun undefinedFlagUsesCallerDefault() {
        assertTrue(PostHog.isFeatureEnabled("undefined", defaultValue = true))
        assertFalse(PostHog.isFeatureEnabled("undefined", defaultValue = false))
        assertFalse(PostHog.isFeatureEnabled("undefined"))
    }

    @Test
    fun nullFlagUsesCallerDefault() {
        assertTrue(PostHog.isFeatureEnabled("null", defaultValue = true))
        assertFalse(PostHog.isFeatureEnabled("null", defaultValue = false))
    }

    @Test
    fun enabledFlagOverridesFalseDefault() {
        assertTrue(PostHog.isFeatureEnabled("enabled", defaultValue = false))
    }

    @Test
    fun disabledFlagOverridesTrueDefault() {
        assertFalse(PostHog.isFeatureEnabled("disabled", defaultValue = true))
    }
}

private fun createFeatureFlagFake(): PostHogJsApi = js(
    """({
        isFeatureEnabled(key) {
            if (key === 'enabled') return true;
            if (key === 'disabled') return false;
            if (key === 'null') return null;
            return undefined;
        }
    })"""
)
