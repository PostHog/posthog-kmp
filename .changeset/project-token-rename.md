---
"posthog-kmp": minor
---

**Breaking:** rename `PostHogConfig.apiKey` to `projectToken`; `apiKey` still compiles with a deprecation warning, but `copy(apiKey = ...)` must change to `copy(projectToken = ...)`
