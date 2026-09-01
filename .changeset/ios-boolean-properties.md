---
"posthog-kmp": patch
---

Fix boolean properties being sent as `1`/`0` instead of `true`/`false` on iOS — filters should match both (`true` or `1`) until the values written by earlier versions are replaced, which for person, group and super properties only happens when they are set again.
