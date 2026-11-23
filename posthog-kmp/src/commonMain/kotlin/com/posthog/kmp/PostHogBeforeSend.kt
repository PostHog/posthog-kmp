package com.posthog.kmp

/**
 * Event passed to [PostHogBeforeSend] callbacks before it is queued.
 *
 * Return a copied event to change its name, distinct ID, properties or options without mutating the
 * original event. Platform metadata not exposed here, such as the timestamp and UUID, is preserved.
 *
 * On iOS and macOS, a number the native SDK stored, such as a value set with [PostHog.register] or
 * [PostHog.registerOptions], arrives as a `Long` even if you passed an `Int`, so compare numbers by
 * value, for example `(value as? Number)?.toLong() == 7L`.
 *
 * @property options Settings that tell PostHog how to process the event, keyed by option key (see
 *   [PostHogEventOptions]), including the ones the SDK set, such as
 *   [PostHogEventOptions.PROCESS_PERSON_PROFILE] `false` for an anonymous user's event with the
 *   default [PostHogConfig.personProfiles]. Removing a key, or setting it to `null`, removes the
 *   option. A callback that returns an event with `options = null` keeps the options of the event
 *   it was given. On JavaScript and Wasm, posthog-js has no options, so this is always `null` there
 *   and changing it has no effect.
 */
public class PostHogEvent(
    public val event: String,
    public val distinctId: String,
    public val properties: Map<String, Any?>,
    public val options: Map<String, Any?>? = null
) {
    /**
     * Returns a copy of this event with the changes [configure] makes, for example
     * `event.copy { properties = properties - "email" }`.
     */
    public fun copy(configure: Builder.() -> Unit): PostHogEvent = Builder(this).apply(configure).build()

    /** Changes a copy of a [PostHogEvent]. Each property starts with the original event's value. */
    @PostHogDsl
    public class Builder internal constructor(source: PostHogEvent) {
        public var event: String = source.event
        public var distinctId: String = source.distinctId
        public var properties: Map<String, Any?> = source.properties
        public var options: Map<String, Any?>? = source.options

        internal fun build(): PostHogEvent = PostHogEvent(event, distinctId, properties, options)
    }

    override fun equals(other: Any?): Boolean =
        other is PostHogEvent && event == other.event && distinctId == other.distinctId &&
            properties == other.properties && options == other.options

    override fun hashCode(): Int {
        var result = event.hashCode()
        result = 31 * result + distinctId.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + (options?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "PostHogEvent(event=$event, distinctId=$distinctId, properties=$properties, options=$options)"
}

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
        current = if (processed.options == null) processed.copy { options = current.options } else processed
    }
    return current
}

/**
 * The log line for a callback that threw: it names the event and the exception type, never the
 * exception message, which can hold the data the callback was removing.
 */
internal fun beforeSendFailureMessage(event: PostHogEvent, error: Throwable): String =
    "Before-send callback threw ${error::class.simpleName ?: "an exception"}; event \"${event.event}\" was dropped."
