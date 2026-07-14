---
posthog-kmp: minor
---

Add a JVM (desktop) target backed by `com.posthog:posthog`, the pure-JVM core library the Android SDK is built on. Event capture, identification, feature flags, groups, error tracking and super properties are fully supported; the offline event queue and identity cache persist under `~/.posthog-kmp`. Mobile/browser-only options (session recording, lifecycle events, screen-view autocapture, deep links) are ignored on the JVM.
