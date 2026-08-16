@file:OptIn(ExperimentalNativeApi::class)

package com.posthog.kmp.sample

import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.processUnhandledException

actual fun getPlatformName(): String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

actual fun crashSampleApp(throwable: Throwable) {
    processUnhandledException(throwable)
}
