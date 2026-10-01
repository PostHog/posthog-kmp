@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.posthog.kmp

import platform.Foundation.NSLog
import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig

internal actual fun NativePostHogConfig.configurePlatformCapture(config: PostHogConfig) {
    if (config.debug && config.sessionRecording?.enabled == true) {
        NSLog("[PostHog] Session recording is not supported on native macOS. This setting will be ignored.")
    }
    if (config.debug && config.autocapture) {
        NSLog("[PostHog] Autocapture is not supported on native macOS. This setting will be ignored.")
    }
}
