---
posthog-kmp: patch
---

Migrate the Android target to the `com.android.kotlin.multiplatform.library` plugin (replacing `com.android.library`), ahead of AGP 9.0 dropping support for the old combination. The published Android artifact now has a single variant instead of a release build type variant.
