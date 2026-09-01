---
"posthog-kmp": patch
---

Fix boolean properties being sent as `1`/`0` instead of `true`/`false` on iOS — existing filters need the property type set to Boolean under Data management, and anonymous events now correctly skip person profiles when `personProfiles` is `identifiedOnly`.
