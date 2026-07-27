@file:Suppress("MatchingDeclarationName")

package com.posthog.kmp

/** No platform-specific context is required for Kotlin/Wasm. */
public actual class PostHogContext internal constructor(
    @Suppress("unused") private val unit: Unit = Unit
)

/** Creates an empty PostHog context for Kotlin/Wasm. */
public actual fun PostHogContext(): PostHogContext = PostHogContext(Unit)
