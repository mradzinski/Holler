import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.roborazzi) apply false
    // Applied here so the root build scripts are formatted too; modules get it from the block below.
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlint)
}

// Code quality for every module: ktlint checks formatting, detekt checks the code itself.
// Both run as part of `./gradlew build` (through `check`). Fix formatting with `./gradlew ktlintFormat`.
subprojects {
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }
    // In a multiplatform module the plain `detekt` task has no sources of its own: detekt creates one
    // task per source set instead. Make `detekt` run all of them, so one command covers every module.
    // Generated code (for example Compose resource accessors) is not ours to fix.
    tasks.withType<Detekt>().configureEach { exclude { it.file.path.contains("/build/") } }
    // Task names are filtered lazily, so no unrelated task is created just to be inspected.
    tasks.named("detekt") {
        dependsOn(
            provider {
                tasks.names.filter { it.startsWith("detekt") && it.endsWith("SourceSet") && !it.startsWith("detektBaseline") }
            },
        )
    }
    extensions.configure<KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint)
        // Generated code (for example Compose resource accessors) is not ours to format.
        filter { exclude { it.file.path.contains("/build/") } }
    }
}

// One command to publish the library to Maven Central: ./gradlew publishLibrary
// It refuses to start, with a clear message, when the one-time setup (Central Portal token, signing key) is
// incomplete. It publishes only after everything below has passed, so a failing test stops it.
val publishRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "publishLibrary" }

// What must pass before the library is published:
//  - `check` of the library: its tests on every platform (desktop, Android, iOS simulator, Wasm and JS in
//    Chrome), lint and the coverage limit;
//  - the Android screenshots compared with the pictures kept in the repository, which a plain build does not do.
// The sample is not part of it: it is not what gets published.
val checksBeforePublishing = listOf(
    ":holler:check",
    ":holler:verifyRoborazziAndroidHostTest",
)

if (publishRequested) {
    val problems = buildList {
        listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey").forEach { name ->
            if (!providers.gradleProperty(name).isPresent) add("Missing '$name' in ~/.gradle/gradle.properties.")
        }
    }
    check(problems.isEmpty()) {
        "Cannot publish yet:\n" + problems.joinToString("\n") { " - $it" }
    }

    // Depending on the checks is not enough: Gradle may run tasks that do not depend on each other in any
    // order. Every publishing task of the library therefore waits for the checks.
    project(":holler") {
        tasks
            .matching { it.name.startsWith("publish") }
            .configureEach { mustRunAfter(checksBeforePublishing) }
    }
}

tasks.register("publishLibrary") {
    group = "publishing"
    description = "Runs every test, compares the Android screenshots, then publishes the library to Maven Central and releases it."
    dependsOn(checksBeforePublishing)
    dependsOn(":holler:publishAndReleaseToMavenCentral")
}
