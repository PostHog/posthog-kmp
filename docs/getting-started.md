# Getting started

The Kotlin Multiplatform SDK runs in shared Kotlin code on Android, iOS, macOS, the web (JS and Wasm) and the JVM. It delegates to the native PostHog SDK on each target.

To send your first event, add the dependency, then call `PostHog.setup` once at app startup with your project token and host from your [project settings](https://app.posthog.com/project/settings).

Add the SDK to your shared module's `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.posthog:posthog-kmp:<version>")
        }
    }
}
```

On iOS and macOS, the SDK links `posthog-ios` with Kotlin's SwiftPM integration. Run the linkage task once and commit what it generates:

```bash
XCODEPROJ_PATH="$PWD/iosApp/iosApp.xcodeproj" ./gradlew :shared:integrateLinkagePackage
```

Initialize it once. On Android, call it from `Application.onCreate` and pass the `Application`. On every other platform, use `PostHogContext()`:

```kotlin
PostHog.setup(
    config = PostHogConfig(projectToken = "<ph_project_token>", host = "<ph_client_api_host>"),
    context = PostHogContext(this), // PostHogContext() outside Android
)
```

Capture an event and identify the user:

```kotlin
PostHog.capture("button_clicked", properties = mapOf("plan" to "pro"))
PostHog.identify("user-123", userProperties = mapOf("email" to "max@example.com"))
```

Flush queued events before the app process ends:

```kotlin
PostHog.flush()
PostHog.close()
```
