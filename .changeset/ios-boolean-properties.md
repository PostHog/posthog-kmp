---
"posthog-kmp": patch
---

Fix boolean event properties being sent as `1`/`0` instead of `true`/`false` on iOS — events captured by earlier versions keep the numeric values, so filters and breakdowns on those properties should match both (`true` or `1`, `false` or `0`) until that data ages out.
