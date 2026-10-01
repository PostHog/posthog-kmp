@file:Suppress("MatchingDeclarationName")

package com.posthog.kmp

/**
 * Apple PostHog context.
 * No platform-specific context is required on iOS or macOS.
 */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/**
 * Creates a PostHogContext for iOS or macOS.
 */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
