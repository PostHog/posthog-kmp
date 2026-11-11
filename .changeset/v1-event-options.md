---
"posthog-kmp": minor
---

- Add `PostHogEventOptions` to set `processPersonProfile`, `cookielessMode`, `disableSkewCorrection` and `productTourId` per event, through `CaptureOptions.eventOptions` and a new `eventOptions` parameter on `screen()`, `identify()`, `alias()`, `group()`, `setPersonProperties()` and `captureException()` — on JavaScript and Wasm they're sent as their legacy `$` properties on `capture()`, `screen()` and `captureException()` and dropped by the other calls
- Add `PostHog.registerOptions()` and `PostHog.unregisterOptions()` to send event options with every later event
- Add `PostHogEvent.options` so `beforeSend` callbacks can read and change an event's options on Android, JVM, iOS and macOS
- Add `PostHog.captureAi()` to send AI observability events, such as `$ai_generation`, to PostHog's AI endpoint on Android, JVM, iOS and macOS, returning the event's UUID; it sends nothing on JavaScript and Wasm
