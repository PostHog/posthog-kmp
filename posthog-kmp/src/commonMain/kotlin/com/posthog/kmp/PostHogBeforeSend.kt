package com.posthog.kmp

/**
 * Event passed to [PostHogBeforeSend] callbacks before it is queued.
 *
 * Return a copied event to change its name, distinct ID, or properties without mutating the
 * original event. Platform metadata not exposed here, such as the timestamp and UUID, is preserved.
 */
public data class PostHogEvent(
    val event: String,
    val distinctId: String,
    val properties: Map<String, Any?>
)

/**
 * Synchronous hook invoked before an event is queued.
 *
 * Callbacks run in configuration order. Return the same or a modified [PostHogEvent] to continue,
 * or `null` to drop the event and skip the remaining callbacks. If a callback throws, its changes
 * are ignored and the remaining callbacks continue with the last valid event.
 */
public fun interface PostHogBeforeSend {
    public fun run(event: PostHogEvent): PostHogEvent?
}

internal fun PostHogConfig.runBeforeSend(
    event: PostHogEvent,
    onError: (Throwable) -> Unit = {}
): PostHogEvent? {
    var current = event
    for (callback in beforeSend) {
        current = try {
            callback.run(current) ?: return null
        } catch (error: Throwable) {
            onError(error)
            current
        }
    }
    return current
}
