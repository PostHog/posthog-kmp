# PostHog uses Gson reflection for its queue and API payloads.
-keepattributes Signature,*Annotation*
-keep class com.posthog.** { *; }
-keep class com.google.gson.** { *; }

# OkHttp probes optional platform and TLS provider classes at runtime.
-dontwarn android.**
-dontwarn dalvik.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.jetbrains.SharedTextures

-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz
