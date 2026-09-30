#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "$0")/.." && pwd)
target=${1:-iosSimulatorArm64}
case "$target" in
    iosSimulatorArm64) framework_task=linkDebugFrameworkIosSimulatorArm64 ;;
    macosArm64) framework_task=linkDebugFrameworkMacosArm64 ;;
    *) echo "Usage: $0 [iosSimulatorArm64|macosArm64]" >&2; exit 1 ;;
esac
version="$(sed -n 's/^VERSION_MAJOR=//p' "$root_dir/version.properties").$(sed -n 's/^VERSION_MINOR=//p' "$root_dir/version.properties").$(sed -n 's/^VERSION_PATCH=//p' "$root_dir/version.properties")"
kotlin_version=$(sed -n 's/^kotlin = "\([^"]*\)"/\1/p' "$root_dir/gradle/libs.versions.toml")
posthog_ios_version=$(sed -n 's/^posthog-ios = "\([^"]*\)"/\1/p' "$root_dir/gradle/libs.versions.toml")
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT
local_repo="$tmp_dir/repository"
consumer="$tmp_dir/consumer"

"$root_dir/gradlew" -p "$root_dir" \
    :posthog-kmp:publishKotlinMultiplatformPublicationToMavenLocal \
    :posthog-kmp:publishIosSimulatorArm64PublicationToMavenLocal \
    :posthog-kmp:publishIosArm64PublicationToMavenLocal \
    :posthog-kmp:publishMacosArm64PublicationToMavenLocal \
    -Dmaven.repo.local="$local_repo" \
    --no-daemon \
    --no-configuration-cache

swiftpm_metadata="$local_repo/com/posthog/posthog-kmp/$version/posthog-kmp-$version-swiftpm-metadata.json"
python3 - "$swiftpm_metadata" "$posthog_ios_version" <<'PY'
import json
import sys

metadata_path, expected_version = sys.argv[1:]
with open(metadata_path) as metadata_file:
    metadata = json.load(metadata_file)

dependencies = metadata.get("dependencies", [])
if len(dependencies) != 1:
    raise SystemExit(f"Expected exactly one published SwiftPM dependency, found {len(dependencies)}")

dependency = dependencies[0]
if dependency.get("repository", {}).get("value") != "https://github.com/PostHog/posthog-ios.git":
    raise SystemExit("Published SwiftPM metadata does not reference posthog-ios")
if dependency.get("version", {}).get("value") != expected_version:
    raise SystemExit("Published SwiftPM metadata has the wrong posthog-ios version")
if [product.get("name") for product in dependency.get("products", [])] != ["PostHog"]:
    raise SystemExit("Published SwiftPM metadata does not link the PostHog product")
if "absolutePath" in json.dumps(metadata):
    raise SystemExit("Published SwiftPM metadata contains a producer-local path")
PY

# Ensure the consumer cannot accidentally use producer-local SwiftPM outputs referenced by
# the generated cinterop KLIB. The published SwiftPM metadata must recreate them independently.
rm -rf "$root_dir/posthog-kmp/build/kotlin" "$root_dir/build/kotlin"

mkdir -p "$consumer/src/commonMain/kotlin" "$consumer/src/commonTest/kotlin"
cat > "$consumer/settings.gradle.kts" <<EOF
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("$local_repo") }
        mavenCentral()
    }
}

rootProject.name = "posthog-kmp-apple-link-test"
EOF

cat > "$consumer/build.gradle.kts" <<EOF
plugins {
    kotlin("multiplatform") version "$kotlin_version"
}

kotlin {
    iosArm64()
    iosSimulatorArm64 { binaries.framework() }
    macosArm64 { binaries.framework() }

    sourceSets {
        commonMain.dependencies {
            implementation("com.posthog:posthog-kmp:$version")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
EOF

cat > "$consumer/src/commonMain/kotlin/Analytics.kt" <<'EOF'
import com.posthog.kmp.PostHog

fun captureFromSharedCode() {
    PostHog.capture("native_consumer_event")
}
EOF

cat > "$consumer/src/commonTest/kotlin/PostHogLinkTest.kt" <<'EOF'
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import com.posthog.kmp.PostHogBeforeSend
import kotlin.test.Test
import kotlin.test.assertTrue

class PostHogLinkTest {
    @Test
    fun linksPostHogSwiftPackage() {
        var captured = false
        PostHog.setup(
            config = PostHogConfig(
                apiKey = "test",
                host = "http://127.0.0.1:9",
                preloadFeatureFlags = false,
                captureApplicationLifecycleEvents = false,
                beforeSend = listOf(PostHogBeforeSend { event ->
                    if (event.event == "native_consumer_event") captured = true
                    null
                }),
            ),
            context = PostHogContext(),
        )
        try {
            captureFromSharedCode()
            assertTrue(captured)
        } finally {
            PostHog.close()
        }
    }
}
EOF

"$root_dir/gradlew" -p "$consumer" compileAppleMainKotlinMetadata "$framework_task" "${target}Test" --no-daemon --console=plain

test_binary="$consumer/build/bin/$target/debugTest/test.kexe"
if /usr/bin/nm -m "$test_binary" | grep -Eq '\(undefined\).*(PostHog|swiftCompatibility)'; then
    echo "Published $target library leaves PostHog or Swift compatibility symbols unresolved:" >&2
    /usr/bin/nm -m "$test_binary" | grep -E 'PostHog|swiftCompatibility' >&2
    exit 1
fi

consumer_packages="$consumer/build/kotlin/swiftImport"
if ! grep -R -Fq 'https://github.com/PostHog/posthog-ios.git' "$consumer_packages" --include='Package.swift' ||
    ! grep -R -Fq 'name: "PostHog"' "$consumer_packages" --include='Package.swift'; then
    echo "Consumer linkage package does not include the transitive PostHog Swift package" >&2
    exit 1
fi
