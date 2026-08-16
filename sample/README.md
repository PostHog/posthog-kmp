# PostHog KMP sample

The sample runs on Android, iOS, Web, and JVM desktop.

## iOS error tracking

The iOS app target generates dSYMs for Debug and Release builds. Its final `Upload PostHog dSYMs` build phase calls [`scripts/upload-posthog-dsyms.sh`](iosApp/scripts/upload-posthog-dsyms.sh). Uploads are opt-in so normal sample and CI builds do not require PostHog credentials.

Install `posthog-cli` 0.7.7 or newer and authenticate locally:

```bash
posthog-cli login
```

CI can authenticate with `POSTHOG_CLI_HOST`, `POSTHOG_CLI_PROJECT_ID`, and `POSTHOG_CLI_API_KEY`. The API key must be a personal API key with error tracking write and organization read scopes, not the `phc_` project token used by the SDK.

Set `POSTHOG_UPLOAD_DSYMS=1` for a Release build to run the upload phase. To upload a Debug simulator dSYM for an end-to-end test, also set `POSTHOG_UPLOAD_DEBUG_SYMBOLS=1`:

```bash
./gradlew :sample:linkDebugFrameworkIosSimulatorArm64

POSTHOG_UPLOAD_DSYMS=1 \
POSTHOG_UPLOAD_DEBUG_SYMBOLS=1 \
xcodebuild \
  -project sample/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  build
```

Set `POSTHOG_INCLUDE_SOURCE=1` to include source context or `POSTHOG_SKIP_ON_CONFLICT=1` to keep an existing symbol set when its content differs.

To verify crash symbolication:

1. Enter the `phc_` project token and initialize PostHog.
2. Tap **Crash Sample App**. Run without an attached debugger so the crash reporter receives the crash.
3. Launch the app again and initialize PostHog with the same token. The SDK processes and sends the pending crash report.
4. Check the crash and uploaded symbol set in PostHog Error Tracking.

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
2. Include the PostHog release rules from [`compose-desktop.pro`](compose-desktop.pro). They preserve reflection-based PostHog and Gson models.

See [`build.gradle.kts`](build.gradle.kts) for the complete configuration.
