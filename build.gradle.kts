plugins {
    // Kotlin Multiplatform
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false

    // Publishing
    alias(libs.plugins.mavenPublish) apply false
}
