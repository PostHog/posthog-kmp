@file:OptIn(ExperimentalSpmForKmpFeature::class)

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.spmforkmp)
}

val swiftCompatibilityLibraries = listOf(
    "swiftCompatibility50",
    "swiftCompatibility51",
    "swiftCompatibility56",
    "swiftCompatibilityConcurrency",
    "swiftCompatibilityDynamicReplacements",
    "swiftCompatibilityPacks"
)
val portableSwiftLinkerOpts = swiftCompatibilityLibraries.joinToString(" ") {
    "-U __swift_FORCE_LOAD_\$_$it"
}
val swiftToolchainDirectory = providers.exec {
    commandLine("/usr/bin/xcrun", "--toolchain", "XcodeDefault", "--find", "swiftc")
}.standardOutput.asText.map { output ->
    File(output.trim()).parentFile.parentFile
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
// valid after publication. Embed the Swift back-deployment libraries in the KLIB and keep only
// portable linker options for their force-load markers.
tasks.matching { it.name.startsWith("cinteropPostHogBridge") }.configureEach {
    val swiftPlatform = when {
        name.endsWith("IosArm64") -> "iphoneos"
        name.endsWith("IosSimulatorArm64") || name.endsWith("IosX64") -> "iphonesimulator"
        else -> error("Unsupported PostHogBridge cinterop target: $name")
    }
    val compatibilityArchives = swiftCompatibilityLibraries.map { library ->
        swiftToolchainDirectory.map { toolchainDirectory ->
            toolchainDirectory.resolve("lib/swift/$swiftPlatform/lib$library.a")
        }
    }

    inputs.property("portableSwiftLinkerOpts", portableSwiftLinkerOpts)
    inputs.property("swiftCompatibilityArchiveNames", swiftCompatibilityLibraries.joinToString(" ") { "lib$it.a" })
    inputs.files(compatibilityArchives)
        .withPropertyName("swiftCompatibilityArchives")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.NONE)

    doLast {
        val linkerOpts = inputs.properties.getValue("portableSwiftLinkerOpts") as String
        val archiveNames = (inputs.properties.getValue("swiftCompatibilityArchiveNames") as String).split(' ')
        val archivesByName = inputs.files.files.filter { it.name in archiveNames }.associateBy { it.name }
        check(archivesByName.keys == archiveNames.toSet()) {
            "Could not resolve every Swift compatibility archive for $name"
        }
        val archives = archiveNames.map(archivesByName::getValue)
        val manifests = outputs.files.asFileTree.matching { include("**/default/manifest") }.files
        check(manifests.isNotEmpty()) { "No cinterop manifest found in outputs of $name" }

        manifests.forEach { manifest ->
            val lines = manifest.readLines()
            listOf("compilerOpts=", "libraryPaths=", "linkerOpts=", "staticLibraries=").forEach { key ->
                check(lines.count { it.startsWith(key) } == 1) {
                    "Expected exactly one $key entry in ${manifest.absolutePath}"
                }
            }

            val nativeTarget = lines.single { it.startsWith("native_targets=") }.substringAfter('=')
            val includedLibraries = manifest.parentFile.resolve("targets/$nativeTarget/included")
            archives.forEach { archive ->
                archive.copyTo(includedLibraries.resolve(archive.name), overwrite = true)
            }

            val archiveNames = archives.joinToString(" ") { it.name }
            val sanitizedManifest = lines.mapNotNull { line ->
                when {
                    line.startsWith("compilerOpts=") -> null
                    line.startsWith("libraryPaths=") -> null
                    line.startsWith("linkerOpts=") -> "linkerOpts=$linkerOpts"
                    line.startsWith("staticLibraries=") -> "$line $archiveNames"
                    else -> line
                }
            }.joinToString("\n", postfix = "\n")
            manifest.writeText(sanitizedManifest)
        }
    }
}
