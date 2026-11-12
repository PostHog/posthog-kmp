package com.posthog.kmp

/**
 * Event passed to [PostHogBeforeSend] callbacks before it is queued.
 *
 * Return a copied event to change its name, distinct ID, properties or options without mutating the
 * original event. Platform metadata not exposed here, such as the timestamp and UUID, is preserved.
 *
 * @property options Settings that tell PostHog how to process the event, keyed by option key (see
 *   [PostHogEventOptions]), including the ones the SDK set, such as
 *   [PostHogEventOptions.PROCESS_PERSON_PROFILE] `false` for an anonymous user's event with the
 *   default [PostHogConfig.personProfiles]. Removing a key, or setting it to `null`, removes the
 *   option. A callback that returns an event with `options = null`, such as one it constructs
 *   instead of copying, keeps the options of the event it was given. On JavaScript and Wasm,
 *   posthog-js has no options, so this is always empty there and changing it has no effect.
 */
public data class PostHogEvent(
    val event: String,
    val distinctId: String,
    val properties: Map<String, Any?>,
    val options: Map<String, Any?>? = null
)

/**
 * Synchronous hook invoked before an event is queued.
 *
 * Callbacks run in configuration order. Return the same or a modified [PostHogEvent] to continue,
 * or `null` to drop the event and skip the remaining callbacks. If a callback throws, the event is
 * dropped and the remaining callbacks are skipped.
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
        val processed = try {
            callback.run(current) ?: return null
        } catch (error: Throwable) {
            onError(error)
            return null
        }
        // An event the callback built without options keeps the ones it was given
        current = if (processed.options == null) processed.copy(options = current.options) else processed
    }
    return current
}

/**
 * The log line for a callback that threw: it names the event and the exception type, never the
 * exception message, which can hold the data the callback was removing.
 */
internal fun beforeSendFailureMessage(event: PostHogEvent, error: Throwable): String =
    "Before-send callback threw ${error::class.simpleName ?: "an exception"}; event \"${event.event}\" was dropped."
