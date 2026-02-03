# PostHog KMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.samuolis/posthog-kmp)](https://central.sonatype.com/artifact/io.github.samuolis/posthog-kmp)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A **Kotlin Multiplatform** SDK for [PostHog](https://posthog.com) analytics, supporting Android, iOS, Web (JS/Wasm), JVM, and macOS.

## Features

- **Event Capture** - Track custom events with properties
- **User Identification** - Identify users and set person properties
- **Feature Flags** - Evaluate feature flags with payloads and detailed results
- **Group Analytics** - Associate users with organizations/teams
- **Screen Tracking** - Track screen/page views
- **Session Recording** - Record user sessions (Android/iOS)
- **Error Tracking** - Capture exceptions with stack traces
- **Opt In/Out** - GDPR-compliant analytics control
- **Super Properties** - Properties sent with every event
- **Session Management** - Access anonymous ID and session ID

## Supported Platforms

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android | ✅ | PostHog Android SDK (native) |
| iOS | ✅ | PostHog iOS SDK (native via SPM) |
| macOS | ⚠️ | Stub (native SDK pending) |
| JVM | ✅ | HTTP API (Ktor/OkHttp) |
| JS (Browser) | ✅ | posthog-js (native) |
| Wasm (Browser) | ✅ | posthog-js (native) |

## Installation

### Gradle (Kotlin DSL)

```kotlin
// In your shared module's build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.samuolis:posthog-kmp:<version>")
        }
    }
}
```

### Version Catalog

```toml
[versions]
posthog-kmp = "<version>"

[libraries]
posthog-kmp = { group = "io.github.samuolis", name = "posthog-kmp", version.ref = "posthog-kmp" }
```

## Quick Start

### Initialize PostHog

```kotlin
import io.github.samuolis.posthog.PostHog
import io.github.samuolis.posthog.PostHogConfig
import io.github.samuolis.posthog.PostHogContext

// Setup with context (recommended)
PostHog.setup(
    config = PostHogConfig(
        apiKey = "phc_your_api_key",
        debug = true
    ),
    context = PostHogContext() // Platform-specific context
)

// Full configuration
PostHog.setup(
    config = PostHogConfig(
        apiKey = "phc_your_api_key",
        host = PostHogConfig.HOST_EU, // or HOST_US (default)
        debug = BuildConfig.DEBUG,
        captureApplicationLifecycleEvents = true,
        captureScreenViews = false,
        preloadFeatureFlags = true
    ),
    context = PostHogContext()
)
```

### Platform-Specific Setup

#### Android

On Android, pass the `Application` context to `PostHogContext`:

```kotlin
// In your Activity or Application
import io.github.samuolis.posthog.PostHog
import io.github.samuolis.posthog.PostHogConfig
import io.github.samuolis.posthog.PostHogContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PostHog.setup(
            config = PostHogConfig(apiKey = "phc_your_api_key"),
            context = PostHogContext(application)
        )
    }
}
```

#### iOS / Web / JVM / macOS

On non-Android platforms, use the no-argument `PostHogContext()`:

```kotlin
// iOS MainViewController.kt
fun MainViewController() = ComposeUIViewController {
    LaunchedEffect(Unit) {
        PostHog.setup(
            config = PostHogConfig(apiKey = "phc_your_api_key"),
            context = PostHogContext()
        )
    }
    App()
}

// Web main.kt
fun main() {
    PostHog.setup(
        config = PostHogConfig(apiKey = "phc_your_api_key"),
        context = PostHogContext()
    )
    // ...
}
```

### Capture Events

```kotlin
// Simple event
PostHog.capture("button_clicked")

// Event with properties
PostHog.capture(
    event = "purchase_completed",
    properties = mapOf(
        "product_id" to "SKU123",
        "price" to 29.99,
        "currency" to "USD"
    )
)

// Event with group context
PostHog.capture(
    event = "feature_used",
    properties = mapOf("feature_name" to "export"),
    options = CaptureOptions(groups = mapOf("company" to "acme_corp"))
)
```

### Identify Users

```kotlin
// Basic identification
PostHog.identify("user_123")

// With user properties
PostHog.identify(
    distinctId = "user_123",
    userProperties = mapOf(
        "email" to "user@example.com",
        "name" to "John Doe",
        "plan" to "premium"
    )
)

// Set properties that won't overwrite existing values
PostHog.identify(
    distinctId = "user_123",
    userPropertiesSetOnce = mapOf(
        "first_seen" to "2025-01-01",
        "signup_source" to "organic"
    )
)

// On logout
PostHog.reset()
```

### Feature Flags

```kotlin
// Check if a feature is enabled
if (PostHog.isFeatureEnabled("new_dashboard")) {
    showNewDashboard()
}

// Get feature flag value (for multivariate flags)
val variant = PostHog.getFeatureFlag("pricing_experiment")
when (variant) {
    "control" -> showOriginalPricing()
    "variant_a" -> showNewPricing()
    "variant_b" -> showPremiumPricing()
}

// Get feature flag payload (JSON data)
val payload = PostHog.getFeatureFlagPayload("config_flags")

// Reload flags (after user properties change)
PostHog.reloadFeatureFlags {
    // Flags are now updated
}

// Get detailed feature flag result (includes reason)
val result = PostHog.getFeatureFlagResult("new_feature")
println("Value: ${result.value}, Reason: ${result.reason}")

// Override flags for testing
PostHog.overrideFeatureFlags(mapOf(
    "new_feature" to true,
    "experiment" to "variant_b"
))
```

### Group Analytics

```kotlin
// Associate user with a company
PostHog.group(
    type = "company",
    key = "acme_corp",
    groupProperties = mapOf(
        "name" to "Acme Corporation",
        "plan" to "enterprise",
        "employee_count" to 500
    )
)

// Associate with multiple groups
PostHog.group("company", "acme_corp")
PostHog.group("team", "engineering")
```

### Screen/Page Tracking

```kotlin
// Track screen views
PostHog.screen("Home")

// With properties
PostHog.screen(
    screenName = "Product Details",
    properties = mapOf(
        "product_id" to "SKU123",
        "category" to "Electronics"
    )
)
```

### Error Tracking

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    PostHog.captureException(
        throwable = e,
        level = ExceptionLevel.ERROR,
        additionalProperties = mapOf(
            "context" to "checkout_flow",
            "user_action" to "submit_payment"
        )
    )
}
```

### Session Recording

Session recording is available on Android and iOS platforms.

```kotlin
import io.github.samuolis.posthog.SessionRecordingConfig

// Enable session recording
PostHog.setup(
    config = PostHogConfig(
        apiKey = "phc_your_api_key",
        sessionRecording = SessionRecordingConfig(
            enabled = true,
            maskAllTextInputs = true,  // Mask sensitive text
            maskAllImages = false,
            captureNetworkTelemetry = true,
            captureLogs = true
        )
    ),
    context = PostHogContext()
)

// Enable experimental screenshot mode (Android/iOS)
// WARNING: Screenshots may contain sensitive information
PostHog.setup(
    config = PostHogConfig(
        apiKey = "phc_your_api_key",
        sessionRecording = SessionRecordingConfig(
            enabled = true,
            screenshot = true,  // Use screenshots instead of wireframes
            maskAllTextInputs = true,
            maskAllImages = true  // Recommended when using screenshots
        )
    ),
    context = PostHogContext()
)

// Android-specific options
PostHog.setup(
    config = PostHogConfig(
        apiKey = "phc_your_api_key",
        sessionRecording = SessionRecordingConfig(
            enabled = true,
            captureLogcat = true,      // Capture Android logcat
            debouncerDelayMs = 500L    // Touch event debounce delay
        )
    ),
    context = PostHogContext()
)
```

### Session Management

```kotlin
// Get the anonymous ID (before identification)
val anonymousId = PostHog.getAnonymousId()

// Get the current session ID
val sessionId = PostHog.getSessionId()

// Get the distinct ID (user identifier)
val distinctId = PostHog.getDistinctId()
```

### Super Properties

```kotlin
// Register properties sent with every event
PostHog.register("app_version", "2.0.0")
PostHog.register("platform", "mobile")

// Register multiple at once
PostHog.registerAll(mapOf(
    "environment" to "production",
    "build_number" to 142
))

// Remove a super property
PostHog.unregister("temporary_flag")
```

### Privacy Controls

```kotlin
// Check opt-out status
if (PostHog.isOptedOut()) {
    showConsentBanner()
}

// Opt out (stop all tracking)
PostHog.optOut()

// Opt back in
PostHog.optIn()
```

### Flush & Close

```kotlin
// Force send all queued events
PostHog.flush()

// Clean shutdown (flushes and releases resources)
PostHog.close()
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiKey` | String | **Required** | Your PostHog project API key |
| `host` | String | US Cloud | PostHog instance URL |
| `debug` | Boolean | `false` | Enable debug logging |
| `captureApplicationLifecycleEvents` | Boolean | `true` | Track app open/close |
| `captureScreenViews` | Boolean | `false` | Auto-track screen views |
| `captureDeepLinks` | Boolean | `true` | Track deep link opens |
| `sendFeatureFlagEvent` | Boolean | `true` | Track flag evaluations |
| `preloadFeatureFlags` | Boolean | `true` | Load flags on init |
| `flushAt` | Int | `20` | Events per batch |
| `flushIntervalSeconds` | Int | `30` | Seconds between flushes |
| `maxQueueSize` | Int | `1000` | Max queued events |
| `maxBatchSize` | Int | `50` | Max events per batch |
| `optOut` | Boolean | `false` | Start opted out |
| `personProfiles` | PersonProfiles | `IDENTIFIED_ONLY` | When to create profiles |
| `sessionRecording` | SessionRecordingConfig? | `null` | Session recording settings |
| `autocapture` | Boolean | `false` | Enable automatic event capture |
| `surveys` | SurveysConfig? | `null` | Surveys configuration |
| `enableExceptionAutocapture` | Boolean | `false` | Auto-capture uncaught exceptions |
| `featureFlagRequestTimeoutMs` | Int | `10000` | Feature flag request timeout |

### Session Recording Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable session recording |
| `maskAllTextInputs` | Boolean | `true` | Mask all text input values |
| `maskAllImages` | Boolean | `false` | Mask all images |
| `captureNetworkTelemetry` | Boolean | `true` | Include network requests |
| `captureLogs` | Boolean | `true` | Capture console/system logs |
| `screenshot` | Boolean | `false` | Use screenshots instead of wireframes (experimental) |
| `captureLogcat` | Boolean | `false` | Capture Android logcat (Android only) |
| `debouncerDelayMs` | Long | `500` | Touch event debounce delay (Android only) |

## Platform-Specific Notes

### Android
- Uses official PostHog Android SDK (`posthog-android` 3.30.0+)
- Requires `PostHogContext(application)` with Application context
- Session recording with wireframe or screenshot mode
- Automatic lifecycle tracking
- Deep link capture
- Logcat capture support

### iOS
- Uses official PostHog iOS SDK (3.38.0+) via Swift bridge
- Full native SDK features including:
  - Session recording (wireframe or screenshot mode)
  - Surveys (iOS 15+)
  - Autocapture
  - Native networking and caching
  - Network telemetry capture

### macOS
- Currently stub implementation
- Native SDK support planned for future release

### JVM
- Uses Ktor HTTP client with OkHttp engine
- Suitable for server-side Kotlin applications
- Coroutine-based async operations
- Periodic flush with configurable intervals

### JS/Wasm
- Wraps official posthog-js library (1.328.0+)
- Full browser feature support
- Session recording available
- LocalStorage persistence
- Autocapture support

## Contributing

Contributions are welcome!

### Development Setup

```bash
git clone https://github.com/samuolis/posthog-kmp.git
cd posthog-kmp

# Build all targets
./gradlew build

# Run tests
./gradlew allTests
```

## License

```
MIT License

Copyright (c) 2025 Lukas Samuolis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Acknowledgements

- [PostHog](https://posthog.com) - The open source product analytics platform
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - Cross-platform development

## Resources

- [PostHog Documentation](https://posthog.com/docs)
- [PostHog Feature Flags](https://posthog.com/docs/feature-flags)
