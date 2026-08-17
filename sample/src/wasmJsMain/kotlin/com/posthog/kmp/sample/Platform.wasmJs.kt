package com.posthog.kmp.sample

actual fun getPlatformName(): String = "Web Browser (Wasm)"

actual fun crashSampleApp(throwable: Throwable) {
    throw throwable
}
