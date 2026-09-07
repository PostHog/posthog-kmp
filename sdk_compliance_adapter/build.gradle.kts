plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":posthog-kmp"))
    // Match the pinned core delegate's runtime dependency; do not upgrade its serializer.
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
}

application {
    mainClass.set("com.posthog.compliance.ComplianceAdapterKt")
}

tasks.processResources {
    from(rootProject.file("version.properties"))
}
