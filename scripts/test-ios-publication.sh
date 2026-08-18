#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "$0")/.." && pwd)
version="$(sed -n 's/^VERSION_MAJOR=//p' "$root_dir/version.properties").$(sed -n 's/^VERSION_MINOR=//p' "$root_dir/version.properties").$(sed -n 's/^VERSION_PATCH=//p' "$root_dir/version.properties")"
kotlin_version=$(sed -n 's/^kotlin = "\([^"]*\)"/\1/p' "$root_dir/gradle/libs.versions.toml")
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT
local_repo="$tmp_dir/repository"
consumer="$tmp_dir/consumer"

"$root_dir/gradlew" -p "$root_dir" \
    :posthog-kmp:publishKotlinMultiplatformPublicationToMavenLocal \
    :posthog-kmp:publishIosSimulatorArm64PublicationToMavenLocal \
    -Dmaven.repo.local="$local_repo" \
    --no-daemon \
    --no-configuration-cache

cinterop_klib="$local_repo/com/posthog/posthog-kmp-iossimulatorarm64/$version/posthog-kmp-iossimulatorarm64-$version-cinterop-PostHogBridge.klib"
manifest=$(unzip -p "$cinterop_klib" default/manifest)
if grep -Eq '(^|[=" ])/' <<<"$manifest" || grep -Eq '^(compilerOpts|libraryPaths)=' <<<"$manifest"; then
    echo "Published iOS KLIB contains producer-local paths:" >&2
    echo "$manifest" >&2
    exit 1
fi

mkdir -p "$consumer/src/commonTest/kotlin"
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

rootProject.name = "posthog-kmp-ios-link-test"
EOF

cat > "$consumer/build.gradle.kts" <<EOF
plugins {
    kotlin("multiplatform") version "$kotlin_version"
}

kotlin {
    iosSimulatorArm64()

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

cat > "$consumer/src/commonTest/kotlin/PostHogLinkTest.kt" <<'EOF'
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import kotlin.test.Test

class PostHogLinkTest {
    @Test
    fun linksPostHogIosBridge() {
        PostHog.setup(
            config = PostHogConfig(apiKey = "test", preloadFeatureFlags = false),
            context = PostHogContext(),
        )
        PostHog.close()
    }
}
EOF

"$root_dir/gradlew" -p "$consumer" iosSimulatorArm64Test --no-daemon --console=plain
