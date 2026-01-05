package io.github.samuolis.posthog

import android.app.Application

/**
 * Android-specific PostHog context that wraps the Application.
 *
 * @property application The Android Application instance
 */
public actual class PostHogContext(
    public val application: Application
)

/**
 * Creates a PostHogContext. On Android, this throws an error since
 * Application context is required. Use PostHogContext(application) instead.
 */
public actual fun PostHogContext(): PostHogContext {
    throw IllegalStateException(
        "Android requires Application context. Use PostHogContext(application) instead."
    )
}
