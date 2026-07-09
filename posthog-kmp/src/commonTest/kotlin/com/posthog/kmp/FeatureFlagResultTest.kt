package com.posthog.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeatureFlagResultTest {

    @Test
    fun valueReturnsVariantWhenPresent() {
        val result = FeatureFlagResult(key = "flag", enabled = true, variant = "control")
        assertEquals("control", result.value)
    }

    @Test
    fun valueReturnsEnabledWhenNoVariant() {
        assertEquals(true, FeatureFlagResult(key = "flag", enabled = true).value)
        assertEquals(false, FeatureFlagResult(key = "flag", enabled = false).value)
    }

    @Test
    fun getPayloadAsReturnsPayloadWhenTypeMatches() {
        val result = FeatureFlagResult(key = "flag", enabled = true, payload = "hello")
        assertEquals("hello", result.getPayloadAs<String>())
    }

    @Test
    fun getPayloadAsReturnsNullWhenTypeMismatches() {
        val result = FeatureFlagResult(key = "flag", enabled = true, payload = "hello")
        assertNull(result.getPayloadAs<Int>())
    }

    @Test
    fun getPayloadAsReturnsNullWhenPayloadAbsent() {
        assertNull(FeatureFlagResult(key = "flag", enabled = true).getPayloadAs<String>())
    }
}
