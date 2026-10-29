---
"posthog-kmp": minor
---

**Breaking:** remove `SessionRecordingConfig.screenshot`. posthog-android 4.0 and posthog-ios 4.0 always record masked screenshots, so the flag no longer did anything. The JVM constructor and `copy` overloads kept for the old signature are removed with it
