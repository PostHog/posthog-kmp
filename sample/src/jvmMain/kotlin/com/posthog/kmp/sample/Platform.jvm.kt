package com.posthog.kmp.sample

actual fun getPlatformName(): String = System.getProperty("os.name") ?: "JVM"
