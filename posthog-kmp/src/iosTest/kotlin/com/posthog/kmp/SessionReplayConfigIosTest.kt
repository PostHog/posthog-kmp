@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodGpu
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodStandard
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionReplayConfigIosTest {
    private fun nativeConfigFor(sessionRecording: SessionRecordingConfig): NativePostHogConfig {
        val native = NativePostHogConfig(projectToken = "phc_test", host = "https://us.i.posthog.com")
        native.configurePlatformCapture(PostHogConfig(projectToken = "phc_test", sessionRecording = sessionRecording))
        return native
    }

    // posthog-ios defaults to .standard, so the KMP default has to be passed explicitly.
    @Test
    fun testDefaultCaptureMethodIsGpu() {
        val native = nativeConfigFor(SessionRecordingConfig())
        assertEquals(PostHogSessionReplayCaptureMethodGpu, native.sessionReplayConfig.captureMethod)
    }

    @Test
    fun testStandardCaptureMethodIsApplied() {
        val native = nativeConfigFor(SessionRecordingConfig(iosCaptureMethod = IosCaptureMethod.STANDARD))
        assertEquals(PostHogSessionReplayCaptureMethodStandard, native.sessionReplayConfig.captureMethod)
    }

    @Test
    fun testThrottleDelayIsConvertedToSeconds() {
        val native = nativeConfigFor(SessionRecordingConfig(throttleDelayMs = 2500L))
        assertEquals(2.5, native.sessionReplayConfig.throttleDelay)
    }
}
