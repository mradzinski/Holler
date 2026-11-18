// Named differently from the :holler module, so the generated project accessors do not clash.
rootProject.name = "holler-root"

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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// The library.
include(":holler")

// Sample apps: one shared module with the demo screen, and one entry-point module per platform.
// The iOS entry point is the Xcode project in sample/iosApp, which is not a Gradle module.
include(":sample:shared")
include(":sample:androidApp")
include(":sample:desktopApp")
include(":sample:webApp")
