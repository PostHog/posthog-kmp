package com.posthog.kmp.sample

actual fun getPlatformName(): String = "Android"

actual fun crashSampleApp(throwable: Throwable) {
    throw throwable
}
