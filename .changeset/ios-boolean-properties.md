---
"posthog-kmp": patch
---

Fix boolean event properties being sent as `1`/`0` instead of `true`/`false` on iOS — existing insights and filters written against the numeric values need updating.
