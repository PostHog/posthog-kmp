package com.samuolis.posthog

/**
 * JVM PostHog context.
 * No platform-specific context is required for JVM.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for JVM.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
