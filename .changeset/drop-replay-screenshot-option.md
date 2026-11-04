---
"posthog-kmp": minor
---

- **Breaking:** remove `SessionRecordingConfig.screenshot` and always record session replay as masked screenshots on Android and iOS, which used wireframes by default — mask custom-drawn content such as maps or video, or turn replay off
- **Breaking:** remove the JVM `SessionRecordingConfig` constructor and `copy` overloads that took `screenshot`
