package io.github.samuolis.posthog

/**
 * Platform-specific context required for PostHog initialization.
 *
 * On Android, this wraps the Application context.
 * On other platforms, this is an empty marker object.
 *
 * ## Usage
 *
 * ```kotlin
 * // Android
 * val context = PostHogContext(application)
 *
 * // iOS/Web/JVM/macOS
 * val context = PostHogContext()
 *
 * // Setup PostHog with context
 * PostHog.setup(config, context)
 * ```
 */
public expect class PostHogContext

/**
 * Creates a default PostHogContext for non-Android platforms.
 * On Android, you must use PostHogContext(application) instead.
 */
public expect fun PostHogContext(): PostHogContext
