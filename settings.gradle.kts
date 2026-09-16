pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "posthog-kmp"

include(":posthog-kmp")
include(":sdk_compliance_adapter")
include(":sample")
include(":sample:androidApp")
