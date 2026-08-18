@file:OptIn(ExperimentalSpmForKmpFeature::class)

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import java.util.Properties

val swiftCompatibilitySymbols = listOf(
    "__swift_FORCE_LOAD_\$_swiftCompatibility50",
    "__swift_FORCE_LOAD_\$_swiftCompatibility51",
    "__swift_FORCE_LOAD_\$_swiftCompatibility56",
    "__swift_FORCE_LOAD_\$_swiftCompatibilityConcurrency",
    "__swift_FORCE_LOAD_\$_swiftCompatibilityDynamicReplacements",
    "__swift_FORCE_LOAD_\$_swiftCompatibilityPacks"
)
val portableSwiftLinkerOpts = swiftCompatibilitySymbols.joinToString(" ") { "-U $it" }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.spmforkmp)
}

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
val generatePostHogVersion = tasks.register("generatePostHogVersion") {
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
    explicitApi()

    android {
        namespace = "com.posthog.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 21

        withHostTestBuilder {}

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

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

    js {
        browser {
            webpackTask {
                mainOutputFileName = "posthog-kmp.js"
            }
        }
        nodejs()
        binaries.library()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.library()
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            kotlin.srcDir(generatedVersionDir)
        }

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        // shared delegation to the core PostHogInterface for the two JVM-backed targets
        val jvmCommonMain = create("jvmCommonMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.posthog.core)
            }
        }

        getByName("androidMain") {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.posthog.android)
            }
        }

        val iosMain = create("iosMain") {
            dependsOn(commonMain)
        }
        val iosTest = create("iosTest") {
            dependsOn(commonTest)
        }

        getByName("iosX64Main") { dependsOn(iosMain) }
        getByName("iosX64Test") { dependsOn(iosTest) }
        getByName("iosArm64Main") { dependsOn(iosMain) }
        getByName("iosArm64Test") { dependsOn(iosTest) }
        getByName("iosSimulatorArm64Main") { dependsOn(iosMain) }
        getByName("iosSimulatorArm64Test") { dependsOn(iosTest) }

        getByName("jsMain") {
            dependencies {
                implementation(npm("posthog-js", libs.versions.posthog.js.get()))
            }
        }

        getByName("wasmJsMain") {
            dependencies {
                implementation(npm("posthog-js", libs.versions.posthog.js.get()))
            }
        }

        getByName("wasmJsTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("jvmMain") {
            dependsOn(jvmCommonMain)
            dependencies {
                // Okio 3.6.0 can produce invalid bytecode when optimized by Compose Desktop's ProGuard.
                implementation(libs.okio.jvm)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    // Sign only when a key is configured (CI provides ORG_GRADLE_PROJECT_signingInMemoryKey),
    // so publishToMavenLocal works on dev machines without one.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = SourcesJar.Sources()
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
tasks.matching { it.name.endsWith("sourcesJar", ignoreCase = true) }.configureEach {
    dependsOn(generatePostHogVersion)
}

// spmForKmp needs producer-local paths while generating the cinterop, but those paths are not
// valid after publication. The static Swift archive is already embedded in the KLIB, so keep only
// portable options for its Swift compatibility force-load markers.
tasks.matching { it.name.startsWith("cinteropPostHogBridge") }.configureEach {
    inputs.property("portableSwiftLinkerOpts", portableSwiftLinkerOpts)
    doLast {
        val linkerOpts = inputs.properties.getValue("portableSwiftLinkerOpts") as String
        outputs.files.asFileTree.matching { include("**/default/manifest") }.forEach { manifest ->
            val sanitizedManifest = manifest.readLines().mapNotNull { line ->
                when {
                    line.startsWith("compilerOpts=") -> null
                    line.startsWith("libraryPaths=") -> null
                    line.startsWith("linkerOpts=") -> "linkerOpts=$linkerOpts"
                    else -> line
                }
            }.joinToString("\n", postfix = "\n")
            manifest.writeText(sanitizedManifest)
        }
    }
}
