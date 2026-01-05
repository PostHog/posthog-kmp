package io.github.samuolis.posthog

/**
 * JavaScript PostHog context.
 * No platform-specific context is required for browser/Node.js.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for JavaScript.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
