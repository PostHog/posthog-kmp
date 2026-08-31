---
"posthog-kmp": patch
---

Send boolean event properties as JSON booleans on iOS. They were serialized as `1`/`0`, which broke boolean property filters and split insight breakdowns across `true`/`false`/`1`/`0` buckets for apps shipping the same common code on Android and iOS. Properties set by the native iOS SDK, such as `$is_identified` and `$feature/<flag>`, were affected on every event.
