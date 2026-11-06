---
"posthog-kmp": minor
---

- Add `SessionRecordingConfig.throttleDelayMs`, the minimum delay between session replay snapshots, and apply it on iOS as well as Android
- Deprecate `SessionRecordingConfig.debouncerDelayMs` in favor of `throttleDelayMs`; `copy()` takes only `throttleDelayMs`
