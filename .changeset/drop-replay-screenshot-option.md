---
"posthog-kmp": minor
---

- **Breaking:** remove `SessionRecordingConfig.screenshot`, because posthog-android 4.0 and posthog-ios 4.0 removed wireframe replay — apps that kept the default `false` now record masked screenshots, including Compose screens on Android, so mask custom-drawn content such as maps or video, or turn replay off
- **Breaking:** record Compose Multiplatform screens on iOS, which 0.10 didn't, fully masked while `maskAllTextInputs` or `maskAllImages` is on
- **Breaking:** remove the JVM `SessionRecordingConfig` constructor and `copy` overloads that took `screenshot`
