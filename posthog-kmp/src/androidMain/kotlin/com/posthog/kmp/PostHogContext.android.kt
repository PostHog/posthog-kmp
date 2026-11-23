package com.posthog.kmp

import android.app.Application

/**
 * Android-specific PostHog context that wraps the Application.
 */
public actual class PostHogContext {
    internal val application: Application?

    /** Creates a context for [application], which PostHog needs on Android. */
    public constructor(application: Application) {
        this.application = application
    }

    internal constructor(@Suppress("UNUSED_PARAMETER") noApplication: Unit) {
        application = null
    }
}

/**
 * Creates a context without an Application. On Android, [PostHog.setup] leaves the SDK disabled
 * and logs a warning with it, so use `PostHogContext(application)` instead.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
