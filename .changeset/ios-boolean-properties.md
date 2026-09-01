---
"posthog-kmp": patch
---

Send boolean event properties as JSON booleans on iOS. They were serialized as `1`/`0`, which broke boolean property filters and split insight breakdowns across `true`/`false`/`1`/`0` buckets for apps shipping the same common code on Android and iOS.

Properties set by the native iOS SDK were affected too, whether or not the app captured a boolean itself: `$is_identified`, `$process_person_profile`, `$is_emulator`, `$network_wifi`, `$network_cellular`, `$is_testflight`, `$is_sideloaded`, `$is_ios_running_on_mac`, `$is_mac_catalyst_app` and `$feature/<flag>`. Null-valued properties the native SDK sets, such as `$feature_flag_response` for an absent flag, are no longer dropped either.

Existing iOS events keep their numeric values, so insights, filters and cohorts written against `1`/`0` need updating and will show both buckets until the old data ages out. Boolean super properties registered by an earlier version are stored as numbers on disk and keep going out that way until `register` is called again.
