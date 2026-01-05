# PostHog KMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nicepics/posthog-kmp)](https://central.sonatype.com/artifact/io.github.nicepics/posthog-kmp)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A **Kotlin Multiplatform** SDK for [PostHog](https://posthog.com) analytics, supporting Android, iOS, Web (JS/Wasm), JVM, and macOS.

## Features

- **Event Capture** - Track custom events with properties
- **User Identification** - Identify users and set person properties
- **Feature Flags** - Evaluate feature flags with payloads
- **Group Analytics** - Associate users with organizations/teams
- **Screen Tracking** - Track screen/page views
- **Error Tracking** - Capture exceptions with stack traces
- **Opt In/Out** - GDPR-compliant analytics control
- **Super Properties** - Properties sent with every event

## Supported Platforms

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android | ✅ | PostHog Android SDK (native) |
| iOS | ✅ | PostHog iOS SDK (native via SPM) |
| macOS | ✅ | PostHog iOS/macOS SDK (native via SPM) |
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
            implementation("io.github.nicepics:posthog-kmp:0.1.0")
        }
    }
}
```

### Version Catalog

```toml
[versions]
posthog-kmp = "0.1.0"

[libraries]
posthog-kmp = { group = "io.github.nicepics", name = "posthog-kmp", version.ref = "posthog-kmp" }
```

## Quick Start

### Initialize PostHog

```kotlin
import io.posthog.kmp.PostHog
import io.posthog.kmp.PostHogConfig

// Basic setup
PostHog.setup(PostHogConfig(
    apiKey = "phc_your_api_key"
))

// Full configuration
PostHog.setup(PostHogConfig(
    apiKey = "phc_your_api_key",
    host = PostHogConfig.HOST_EU, // or HOST_US
    debug = BuildConfig.DEBUG,
    captureApplicationLifecycleEvents = true,
    captureScreenViews = false,
    preloadFeatureFlags = true
))
```

### Android Setup

On Android, you must set the application context before calling `setup()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        PostHog.setApplicationContext(this)
        PostHog.setup(PostHogConfig(apiKey = "phc_your_api_key"))
    }
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
| `optOut` | Boolean | `false` | Start opted out |
| `personProfiles` | PersonProfiles | `IDENTIFIED_ONLY` | When to create profiles |

## Platform-Specific Notes

### Android
- Uses official PostHog Android SDK (`posthog-android`)
- Supports session recording
- Automatic lifecycle tracking
- Deep link capture
- Error auto-capture

### iOS/macOS
- Uses official PostHog iOS SDK via [SPM4KMP](https://github.com/AElkhami/spm-for-kmp)
- Full native SDK features including:
  - Session recording (iOS only)
  - Surveys (iOS 15+)
  - Autocapture (iOS only)
  - Native networking and caching
- Swift bridge handles Kotlin/Native interop

### JVM
- Uses Ktor HTTP client with OkHttp engine
- Suitable for server-side Kotlin applications
- Coroutine-based async operations
- Periodic flush with configurable intervals

### JS/Wasm
- Wraps official posthog-js library
- Full browser feature support
- Session recording available
- LocalStorage persistence

## Publishing to Maven Central

This library is configured for publishing to Maven Central using the [vanniktech gradle plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).

### Setup

1. Create a Sonatype OSSRH account at [central.sonatype.com](https://central.sonatype.com)
2. Create a GPG key for signing
3. Configure your `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=your_username
mavenCentralPassword=your_password
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
```

### Publish

```bash
# Publish to Maven Central
./gradlew publishAllPublicationsToMavenCentralRepository

# Publish snapshot
./gradlew publishAllPublicationsToMavenCentralRepository -PVERSION_NAME=0.1.0-SNAPSHOT
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

```bash
git clone https://github.com/nicepics/posthog-kmp.git
cd posthog-kmp

# Build all targets
./gradlew build

# Run tests
./gradlew allTests

# Check code style
./gradlew detekt
```

## License

```
MIT License

Copyright (c) 2025 NicePics

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
- [vanniktech/gradle-maven-publish-plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) - Publishing to Maven Central

## Resources

- [PostHog Documentation](https://posthog.com/docs)
- [PostHog Feature Flags](https://posthog.com/docs/feature-flags)
- [Kotlin Multiplatform Publishing Guide](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries.html)
