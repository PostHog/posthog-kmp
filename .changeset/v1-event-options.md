---
"posthog-kmp": minor
---

- Add event options, set per event as a map through `CaptureOptions.eventOptions` and a new `eventOptions` parameter on `screen()`, `identify()`, `alias()`, `group()`, `setPersonProperties()` and `captureException()` — on JavaScript and Wasm they're sent as their legacy `$` properties on `capture()`, `screen()` and `captureException()` and dropped by the other calls
- Add `PostHogEventOptions` with the option keys `PROCESS_PERSON_PROFILE`, `COOKIELESS_MODE`, `DISABLE_SKEW_CORRECTION` and `PRODUCT_TOUR_ID`; other keys are sent as given on Android, JVM, iOS and macOS
- Add `PostHog.registerOptions()` and `PostHog.unregisterOptions()` to send event options with every later event
- Add `PostHogEvent.options` so `beforeSend` callbacks can read and change an event's options on Android, JVM, iOS and macOS
- Add `PostHog.captureAi()` to send AI observability events, such as `$ai_generation`, to PostHog's AI endpoint on Android, JVM, iOS and macOS, returning the event's UUID; it sends nothing on JavaScript and Wasm
- Add `PostHogConfig.captureAiCompression`, `captureAiMaxQueueSize`, `captureAiTimeout` and `captureAiMaxEventBytes` to configure `captureAi()` requests on Android, JVM, iOS and macOS
- Name the event and the exception type in the debug log when a `beforeSend` callback throws
