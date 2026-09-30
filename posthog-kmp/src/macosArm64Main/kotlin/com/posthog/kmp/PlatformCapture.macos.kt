@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.posthog.kmp

import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig

internal actual fun NativePostHogConfig.configurePlatformCapture(config: PostHogConfig) {
    // Session replay and UIKit interaction autocapture are only available on iOS.
}
