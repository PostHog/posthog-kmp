@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorTrackingConfigAppleTest {
    private fun nativeConfig() = NativePostHogConfig(projectToken = "phc_test", host = "https://us.i.posthog.com")

    @Test
    fun testInAppByDefaultIsTrueWithoutErrorTrackingConfig() {
        val native = nativeConfig()
        native.applyErrorTracking(null)
        assertTrue(native.errorTrackingConfig.inAppByDefault)
        assertFalse(native.errorTrackingConfig.autoCapture)
    }

    @Test
    fun testErrorTrackingConfigIsApplied() {
        val native = nativeConfig()
        val defaultIncludes = native.errorTrackingConfig.inAppIncludes
        native.applyErrorTracking(
            ErrorTrackingConfig(autoCapture = true, inAppIncludes = listOf("Shared"), inAppByDefault = false)
        )
        assertFalse(native.errorTrackingConfig.inAppByDefault)
        assertTrue(native.errorTrackingConfig.autoCapture)
        assertEquals(defaultIncludes + "Shared", native.errorTrackingConfig.inAppIncludes)
    }
}
