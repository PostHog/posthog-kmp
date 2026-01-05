package com.samuolis.posthog

/**
 * WasmJS PostHog context.
 * No platform-specific context is required for WebAssembly.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for WasmJS.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
