import java.io.File

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.posthogAndroid)
}

android {
    namespace = "com.posthog.kmp.sample"
    compileSdk = libs.versions.android.sampleCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.posthog.kmp.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":sample"))
    implementation(libs.androidx.activity.compose)
}

// Mapping generation and map-ID injection remain enabled for every minified build. Upload only when
// posthog-cli has credentials, so normal builds and CI dry runs do not require PostHog access.
val hasPostHogEnvironmentCredentials =
    providers.environmentVariable("POSTHOG_CLI_API_KEY").isPresent &&
        providers.environmentVariable("POSTHOG_CLI_PROJECT_ID").isPresent
val hasPostHogDotenvCredentials =
    providers.environmentVariable("POSTHOG_CLI_DOTENV_FILE").isPresent ||
        providers.gradleProperty("posthog.dotenvFile").isPresent
val hasStoredPostHogCredentials = System.getProperty("user.home")
    ?.let { File(it, ".posthog/credentials.json").isFile }
    ?: false
val postHogUploadEnabled =
    hasPostHogEnvironmentCredentials ||
        hasPostHogDotenvCredentials ||
        (!providers.environmentVariable("CI").isPresent && hasStoredPostHogCredentials)

tasks.withType<com.posthog.android.PostHogUploadProguardMappingsTask>().configureEach {
    isEnabled = postHogUploadEnabled
}
