---
"posthog-kmp": minor
---

- **Breaking:** pass `PostHogFeatureFlagsLoaded` with the loaded flags and `errorsLoading` to the `reloadFeatureFlags()` callback, and call it with `errorsLoading = true` before `setup()` on every platform
- **Breaking:** treat a flag whose variant is an empty string as disabled in `isFeatureEnabled()`, `FeatureFlagResult.enabled`, `getAllFeatureFlags()` and `$active_feature_flags` on every platform
- Add `PostHog.onFeatureFlags()` to listen for feature flag loads, returning a `PostHogFeatureFlagsSubscription`
- Add `setPersonPropertiesForFlags()`, `resetPersonPropertiesForFlags()`, `setGroupPropertiesForFlags()` and `resetGroupPropertiesForFlags()`
- Add `PostHogConfig.evaluationContexts` to evaluate only the feature flags tagged with those contexts
