@file:OptIn(ExperimentalSpmForKmpFeature::class)

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.spmforkmp)
}

// Load version from version.properties
val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val versionMajor = versionProperties["VERSION_MAJOR"] as String
val versionMinor = versionProperties["VERSION_MINOR"] as String
val versionPatch = versionProperties["VERSION_PATCH"] as String
version = "$versionMajor.$versionMinor.$versionPatch"
group = "io.github.samuolis"

kotlin {
    // Explicit API mode - forces visibility modifiers and return types
    explicitApi()

    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }

    // JVM target (for server-side Kotlin usage)
    jvm()

    // iOS targets with SPM4KMP for native PostHog SDK
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.swiftPackageConfig("PostHogBridge") {
            minIos = "13.0"
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/PostHog/posthog-ios.git"),
                    products = {
                        add("PostHog")
                    },
                    version = libs.versions.posthog.ios.get()
                )
            }
        }
    }

    // macOS targets - use JVM HTTP implementation for now
    // Native PostHog SDK support can be added later
    macosX64()
    macosArm64()

    // JavaScript targets
    js(IR) {
        browser {
            webpackTask {
                mainOutputFileName = "posthog-kmp.js"
            }
        }
        nodejs()
        binaries.library()
    }

    // WebAssembly JS target
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    // Source sets configuration
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.posthog.android)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val macosMain by creating {
            dependsOn(commonMain)
        }

        val macosX64Main by getting { dependsOn(macosMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }

        val jsMain by getting {
            dependencies {
                implementation(npm("posthog-js", libs.versions.posthog.js.get()))
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(npm("posthog-js", libs.versions.posthog.js.get()))
            }
        }

        val jvmMain by getting {
            dependencies {
                // JVM uses Ktor for HTTP-based implementation
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
}

android {
    namespace = "io.github.samuolis.posthog"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Maven Central Publishing Configuration
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = true
    ))

    pom {
        name.set(project.findProperty("POM_NAME") as String? ?: "PostHog KMP")
        description.set(project.findProperty("POM_DESCRIPTION") as String? ?: "Kotlin Multiplatform PostHog SDK")
        url.set(project.findProperty("POM_URL") as String? ?: "https://github.com/samuolis/posthog-kmp")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set(project.findProperty("POM_LICENCE_NAME") as String? ?: "MIT License")
                url.set(project.findProperty("POM_LICENCE_URL") as String? ?: "https://opensource.org/licenses/MIT")
                distribution.set(project.findProperty("POM_LICENCE_DIST") as String? ?: "repo")
            }
        }

        developers {
            developer {
                id.set(project.findProperty("POM_DEVELOPER_ID") as String? ?: "samuolis")
                name.set(project.findProperty("POM_DEVELOPER_NAME") as String? ?: "Lukas Samuolis")
                url.set(project.findProperty("POM_DEVELOPER_URL") as String? ?: "https://github.com/samuolis")
            }
        }

        scm {
            url.set(project.findProperty("POM_SCM_URL") as String? ?: "https://github.com/samuolis/posthog-kmp")
            connection.set(project.findProperty("POM_SCM_CONNECTION") as String? ?: "scm:git:git://github.com/samuolis/posthog-kmp.git")
            developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String? ?: "scm:git:ssh://git@github.com/samuolis/posthog-kmp.git")
        }
    }
}
