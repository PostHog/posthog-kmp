---
"posthog-kmp": minor
---

- Add `SessionRecordingConfig.iosCaptureMethod` to choose how iOS captures session replay screenshots. It defaults to `STANDARD`, like posthog-ios; the experimental `GPU` takes less main-thread time but doesn't capture web view pages and records Metal content, including Compose Multiplatform screens, as a placeholder
