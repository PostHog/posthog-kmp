import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false

    alias(libs.plugins.mavenPublish) apply false

    alias(libs.plugins.detekt)
}

// Force patched versions of vulnerable JS build-toolchain transitives; the
// generated kotlin-js-store/yarn.lock is out of dependabot's reach. Remove an
// override once the Kotlin plugin's own pin catches up.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.apply {
        webpack.version = "5.104.1"
        webpackDevServer.version = "5.2.6"
    }
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("serialize-javascript", "7.0.5")
        resolution("uuid", "11.1.1")
        resolution("diff", "8.0.3")
        // Forces 1.x consumers onto the 2.x line (same API), like uuid above.
        resolution("brace-expansion", "2.1.2")
        resolution("fast-uri", "3.1.4")
        resolution("body-parser", "1.20.6")
        resolution("dompurify", "3.4.12")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    val detektBaseline = rootProject.file("config/detekt/baseline.xml")
    if (detektBaseline.exists()) {
        baseline = detektBaseline
    }
    // Lint the whole repo (KMP source sets live outside the default src/main layout).
    source.setFrom(files(rootProject.rootDir))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_17.toString()
    exclude("**/build/**", "**/generated/**", "**/.gradle/**")
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = JavaVersion.VERSION_17.toString()
    exclude("**/build/**", "**/generated/**", "**/.gradle/**")
}
