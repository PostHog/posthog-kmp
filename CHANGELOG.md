# Changelog

All notable changes to `posthog-kmp` are documented here. This file is maintained
by [Sampo](https://github.com/PostHog/sampo) from changesets in `.sampo/changesets/`.

## 0.1.0 — 2026-07-16

### Minor changes

- [8bac955](https://github.com/posthog/posthog-kmp/commit/8bac9551fc7aa9c519ddd6159e29034fe9a7346c) Add a JVM (desktop) target backed by `com.posthog:posthog`, the pure-JVM core library the Android SDK is built on. Event capture, identification, feature flags, groups, error tracking and super properties are fully supported; the offline event queue and identity cache persist under `~/.posthog-kmp`. Mobile/browser-only options (session recording, lifecycle events, screen-view autocapture, deep links) are ignored on the JVM. — Thanks @turnipdabeets!

## 0.0.2 — 2026-07-14

### Patch changes

- [f23ae08](https://github.com/posthog/posthog-kmp/commit/f23ae082b7d64d7e81d96b53e67097f16fd3fda9) Migrate the Android target to the `com.android.kotlin.multiplatform.library` plugin (replacing `com.android.library`), ahead of AGP 9.0 dropping support for the old combination. The published Android artifact now has a single variant instead of a release build type variant. — Thanks @turnipdabeets!

## 0.0.1 — 2026-07-14

### Patch changes

- [cf0125c](https://github.com/posthog/posthog-kmp/commit/cf0125c2a85ec3d958873c08dd3c3c67ed1d35d9) Initial public release of the PostHog Kotlin Multiplatform SDK: event capture, user identification, feature flags, group analytics, screen tracking, error tracking, session recording (Android/iOS), and session management for Android, iOS, and Web (JS). — Thanks @turnipdabeets!

