---
"posthog-kmp": minor
---

- **Breaking:** replace `SessionRecordingConfig.debouncerDelayMs` with `throttleDelayMs`, the minimum delay between session replay snapshots, which now applies on iOS as well as Android
