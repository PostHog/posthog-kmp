@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.posthog.kmp

import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodBackground
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodGpu
import swiftPMImport.com.posthog.posthog.kmp.PostHogSessionReplayCaptureMethodStandard

internal actual fun NativePostHogConfig.configurePlatformCapture(config: PostHogConfig) {
    captureElementInteractions = config.autocapture
    val sessionConfig = config.sessionRecording
    if (sessionConfig?.enabled == true) {
        sessionReplay = true
        sessionReplayConfig.maskAllTextInputs = sessionConfig.maskAllTextInputs
        sessionReplayConfig.maskAllImages = sessionConfig.maskAllImages
        sessionReplayConfig.captureNetworkTelemetry = sessionConfig.captureNetworkTelemetry
        sessionReplayConfig.captureLogs = sessionConfig.captureLogs
        sessionReplayConfig.throttleDelay = sessionConfig.throttleDelayMs / 1000.0
        sessionReplayConfig.captureMethod = when (sessionConfig.iosCaptureMethod) {
            IosCaptureMethod.STANDARD -> PostHogSessionReplayCaptureMethodStandard
            IosCaptureMethod.BACKGROUND -> PostHogSessionReplayCaptureMethodBackground
            IosCaptureMethod.GPU -> PostHogSessionReplayCaptureMethodGpu
        }
    }
}
