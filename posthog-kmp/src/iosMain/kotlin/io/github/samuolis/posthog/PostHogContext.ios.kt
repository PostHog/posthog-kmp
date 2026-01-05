package io.github.samuolis.posthog

/**
 * iOS PostHog context.
 * No platform-specific context is required on iOS.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for iOS.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
