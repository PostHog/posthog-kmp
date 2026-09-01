---
"posthog-kmp": patch
---

Fix boolean properties being sent as `1`/`0` instead of `true`/`false` on iOS — values written by earlier versions do not match filters on the corrected property, so set its type to Boolean under Data management and set any person, group or super property holding one again.
