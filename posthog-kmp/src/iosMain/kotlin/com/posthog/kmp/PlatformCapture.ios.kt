@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.posthog.kmp

import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig

internal actual fun NativePostHogConfig.configurePlatformCapture(config: PostHogConfig) {
    captureElementInteractions = config.autocapture
    val sessionConfig = config.sessionRecording
    if (sessionConfig?.enabled == true) {
        sessionReplay = true
        sessionReplayConfig.maskAllTextInputs = sessionConfig.maskAllTextInputs
        sessionReplayConfig.maskAllImages = sessionConfig.maskAllImages
        sessionReplayConfig.captureNetworkTelemetry = sessionConfig.captureNetworkTelemetry
        sessionReplayConfig.captureLogs = sessionConfig.captureLogs
        sessionReplayConfig.screenshotMode = sessionConfig.screenshot
    }
}
