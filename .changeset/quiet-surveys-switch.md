---
"posthog-kmp": minor
---

Add `surveys` to `PostHogConfig` to turn off surveys on iOS, Android and web. It defaults to `true`, so iOS and web don't change. On Android it now turns on the native surveys integration, which shows surveys only when the app also depends on `com.posthog:posthog-android-surveys-compose`; set it to `false` to keep the previous behavior.
