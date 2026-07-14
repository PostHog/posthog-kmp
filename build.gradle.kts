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
