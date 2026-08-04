# PostHog KMP sample

The sample runs on Android, iOS, Web, and JVM desktop.

## JVM desktop

Run the app from Gradle:

```bash
./gradlew :sample:run
```

Build the macOS release DMG:

```bash
./gradlew :sample:packageReleaseDmg
```

Compose Desktop release distributions use ProGuard and a custom JDK runtime. Apps using PostHog must:

1. Add `jdk.unsupported` to `nativeDistributions.modules`. Gson uses this module to deserialize PostHog queue and API models.
2. Include the PostHog release rules from [`compose-desktop.pro`](compose-desktop.pro). They preserve reflection-based models and prevent ProGuard from producing invalid Okio bytecode.

See [`build.gradle.kts`](build.gradle.kts) for the complete configuration.
