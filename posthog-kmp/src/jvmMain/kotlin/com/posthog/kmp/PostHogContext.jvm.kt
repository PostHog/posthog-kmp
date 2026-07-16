package com.posthog.kmp

/**
 * JVM (desktop/server) PostHog context.
 * No platform-specific context is required for the JVM.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for the JVM.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
