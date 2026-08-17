package com.posthog.kmp.sample

actual fun getPlatformName(): String = "Web Browser (JS)"

actual fun crashSampleApp(throwable: Throwable) {
    throw throwable
}
