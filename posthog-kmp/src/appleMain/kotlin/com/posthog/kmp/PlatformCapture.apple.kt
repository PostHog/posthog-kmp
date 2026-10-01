@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.posthog.kmp

import swiftPMImport.com.posthog.posthog.kmp.PostHogConfig as NativePostHogConfig

internal expect fun NativePostHogConfig.configurePlatformCapture(config: PostHogConfig)
