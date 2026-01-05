package com.samuolis.posthog

/**
 * macOS PostHog context.
 * No platform-specific context is required for macOS.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for macOS.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
