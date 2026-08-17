# Preserve the package prefix used by ErrorTrackingConfig.inAppIncludes after R8 obfuscation.
-keeppackagenames com.posthog.kmp.sample.**

# Keep app methods from being inlined into vendor classes while still allowing shrinking and obfuscation.
-keep,allowshrinking,allowobfuscation class com.posthog.kmp.sample.** {
    *;
}
