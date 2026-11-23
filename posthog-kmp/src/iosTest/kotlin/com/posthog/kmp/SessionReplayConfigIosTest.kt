@file:OptIn(ExperimentalForeignApi::class)

package com.posthog.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodGpu
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodStandard
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionReplayConfigIosTest {
    private fun nativeConfigFor(sessionReplay: SessionReplayConfig): NativePostHogConfig {
        val native = NativePostHogConfig(projectToken = "phc_test", host = "https://us.i.posthog.com")
        native.configurePlatformCapture(PostHogConfig(projectToken = "phc_test", sessionReplay = sessionReplay))
        return native
    }

    @Test
    fun testDefaultCaptureMethodIsStandard() {
        val native = nativeConfigFor(SessionReplayConfig())
        assertEquals(PostHogSessionReplayCaptureMethodStandard, native.sessionReplayConfig.captureMethod)
    }

    // posthog-ios defaults to .standard, so only an opt-in to GPU shows the mapping is passed through.
    @Test
    fun testGpuCaptureMethodIsApplied() {
        val native = nativeConfigFor(SessionReplayConfig(iosCaptureMethod = IosCaptureMethod.GPU))
        assertEquals(PostHogSessionReplayCaptureMethodGpu, native.sessionReplayConfig.captureMethod)
    }

    @Test
    fun testThrottleDelayIsConvertedToSeconds() {
        val native = nativeConfigFor(SessionReplayConfig(throttleDelayMs = 2500L))
        assertEquals(2.5, native.sessionReplayConfig.throttleDelay)
    }
}
