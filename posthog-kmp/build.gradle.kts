@file:OptIn(ExperimentalSpmForKmpFeature::class)

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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

// Generate a common Kotlin source exposing the SDK version (single source of truth:
// version.properties) so platform implementations can report it to PostHog.
val generatedVersionDir = layout.buildDirectory.dir("generated/posthogVersion/kotlin")
val generatePostHogVersion by tasks.registering {
    val versionValue = version.toString()
    val outputDir = generatedVersionDir
    inputs.property("version", versionValue)
    outputs.dir(outputDir)
    doLast {
        val pkgDir = outputDir.get().dir("com/posthog/kmp").asFile
        pkgDir.mkdirs()
        pkgDir.resolve("PostHogKmpVersion.kt").writeText(
            """
            |package com.posthog.kmp
            |
            |/** Generated from version.properties; do not edit by hand. */
            |internal object PostHogKmpVersion {
            |    /** Current posthog-kmp SDK version (e.g. "$versionValue"). */
            |    const val VERSION: String = "$versionValue"
            |}
            |
            """.trimMargin()
        )
    }
}

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

    // Source sets configuration
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedVersionDir)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
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

        val jsMain by getting {
            dependencies {
                implementation(npm("posthog-js", libs.versions.posthog.js.get()))
            }
        }
    }
}

android {
    namespace = "com.posthog.kmp"
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
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = true
    ))

    pom {
        name.set(project.findProperty("POM_NAME") as String? ?: "PostHog KMP")
        description.set(project.findProperty("POM_DESCRIPTION") as String? ?: "Kotlin Multiplatform PostHog SDK")
        url.set(project.findProperty("POM_URL") as String? ?: "https://github.com/PostHog/posthog-kmp")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set(project.findProperty("POM_LICENCE_NAME") as String? ?: "MIT License")
                url.set(project.findProperty("POM_LICENCE_URL") as String? ?: "https://opensource.org/licenses/MIT")
                distribution.set(project.findProperty("POM_LICENCE_DIST") as String? ?: "repo")
            }
        }

        organization {
            name.set("PostHog")
            url.set("https://posthog.com")
        }

        developers {
            developer {
                id.set(project.findProperty("POM_DEVELOPER_ID") as String? ?: "posthog")
                name.set(project.findProperty("POM_DEVELOPER_NAME") as String? ?: "PostHog")
                email.set(project.findProperty("POM_DEVELOPER_EMAIL") as String? ?: "engineering@posthog.com")
                url.set(project.findProperty("POM_DEVELOPER_URL") as String? ?: "https://posthog.com")
                organization.set("PostHog")
                organizationUrl.set("https://posthog.com")
            }
        }

        scm {
            url.set(project.findProperty("POM_SCM_URL") as String? ?: "https://github.com/PostHog/posthog-kmp")
            connection.set(project.findProperty("POM_SCM_CONNECTION") as String? ?: "scm:git:git://github.com/PostHog/posthog-kmp.git")
            developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String? ?: "scm:git:ssh://git@github.com/PostHog/posthog-kmp.git")
        }
    }
}

// Ensure the generated version source exists before any Kotlin compilation / source jar.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generatePostHogVersion)
}
tasks.matching { it.name.endsWith("SourcesJar") }.configureEach {
    dependsOn(generatePostHogVersion)
}
