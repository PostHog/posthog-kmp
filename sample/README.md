# PostHog KMP sample

The sample runs on Android, iOS, Web, and JVM desktop.

## Web error tracking

The Kotlin/JS production build generates a minified webpack bundle and source map. Build it, inject PostHog chunk metadata, and upload the source map with:

```bash
./gradlew :sample:jsBrowserDistribution
sample/scripts/upload-posthog-sourcemaps.sh
```

The upload script uses `com.posthog.kmp.sample.web` as the release name and the current Git commit as its version. Override these with `POSTHOG_RELEASE_NAME` and `POSTHOG_RELEASE_VERSION` when needed. CI can authenticate with `POSTHOG_CLI_HOST`, `POSTHOG_CLI_PROJECT_ID`, and `POSTHOG_CLI_API_KEY`.

Serve the injected production assets rather than rebuilding them after upload:

```bash
python3 -m http.server 8080 --directory sample/build/dist/js/productionExecutable
```

Enter the `phc_` project token, initialize PostHog, then use **Capture Sample Exception** and **Flush** to verify browser stack-trace demangling in PostHog Error Tracking.

## Android error tracking

Release builds use R8 minification and the PostHog Android Gradle plugin to inject a mapping ID into the APK and upload the generated ProGuard mapping to PostHog. The sample preserves its package prefix and prevents app methods from being inlined into vendor classes because Android evaluates `inAppIncludes` against runtime class names before server-side symbolication.

Install `posthog-cli` 0.7.4 or newer and authenticate locally:

```bash
posthog-cli login
./gradlew :sample:androidApp:assembleRelease
```

CI can authenticate with `POSTHOG_CLI_HOST`, `POSTHOG_CLI_PROJECT_ID`, and `POSTHOG_CLI_API_KEY`. The API key must be a personal API key with error tracking write and organization read scopes, not the `phc_` project token used by the SDK.

To verify symbolication, install the minified release build and run it without an attached debugger:

```bash
./gradlew :sample:androidApp:installRelease
```

1. Enter the `phc_` project token and initialize PostHog.
2. Tap **Capture Sample Exception**, then **Flush**, to test a handled exception, or tap **Crash Sample App** to test an unhandled exception.
3. After an unhandled exception, launch the app again and initialize PostHog with the same token so the SDK can send the pending crash report.
4. Check the exception and uploaded mapping in PostHog Error Tracking.

## iOS integration

The Xcode project uses Kotlin's direct framework integration and the generated `KotlinMultiplatformLinkedPackage` to link the transitive `posthog-ios` Swift package and copy its privacy manifests into the app. If the SwiftPM dependencies or Gradle project structure change, regenerate and commit the linkage package and Xcode project changes:

```bash
XCODEPROJ_PATH="$PWD/sample/iosApp/iosApp.xcodeproj" \
GRADLE_PROJECT_PATH=':sample' \
./gradlew :sample:integrateEmbedAndSign :sample:integrateLinkagePackage
```

## iOS error tracking

The iOS app target generates dSYMs for Debug and Release builds. Its `Upload PostHog dSYMs` post-build action calls [`scripts/upload-posthog-dsyms.sh`](iosApp/scripts/upload-posthog-dsyms.sh) after Xcode finishes generating the dSYM. Uploads are opt-in so normal sample and CI builds do not require PostHog credentials.

Install `posthog-cli` 0.7.12 or newer and authenticate locally:

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

Compose Desktop's default release configuration disables obfuscation, so packaged DMGs retain readable class names, source filenames, and line numbers without uploading a mapping. PostHog KMP does not currently support deobfuscating custom-obfuscated JVM desktop builds because it does not expose the ProGuard map ID required for mapping uploads.

Compose Desktop release distributions use ProGuard and a custom JDK runtime. Apps using PostHog must:

1. Add `jdk.unsupported` to `nativeDistributions.modules`. Gson uses this module to deserialize PostHog queue and API models.
2. Include the PostHog release rules from [`compose-desktop.pro`](compose-desktop.pro). They preserve reflection-based PostHog and Gson models.

See [`build.gradle.kts`](build.gradle.kts) for the complete configuration.
