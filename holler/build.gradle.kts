import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

// The pictures the Android screenshot tests compare against are part of the sources, not of the build output.
roborazzi {
    outputDir.set(file("src/androidHostTest/screenshots"))
}

kotlin {
    explicitApi()

    // The tests use one `expect` class, the base class that carries Android's test runner. Kotlin marks
    // such classes as Beta and warns on every build; this says the use is deliberate. The library itself has none.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    android {
        namespace = "io.github.mradzinski.holler"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        // Host (JVM) tests are off by default with this plugin; this runs commonTest on Android too.
        withHostTest { isIncludeAndroidResources = true }
    }
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    js { browser() }

    // The default source sets (iosMain, iosTest and so on) are kept, next to the one added below by hand.
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // "api": these types (Modifier, Color, transitions…) appear in the library's public signatures.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.animation)
            api(libs.compose.ui)
            // The default look comes from the host app's MaterialTheme, and the buttons are Material buttons.
            api(libs.compose.material3)
            // Only used inside the library, for the lock of HollerHostState.
            implementation(libs.kotlinx.atomicfu)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)
        }

        // The tests that draw a message, shared by every target that can run them: desktop, iOS, the web
        // (Wasm) and Android, where Robolectric stands in for a device.
        val uiTest = create("uiTest") {
            dependsOn(commonTest.get())

            dependencies {
                implementation(libs.compose.ui.test)
            }
        }

        // The tests that read the pixels of what was drawn. Robolectric cannot hand a test that picture, so
        // they are shared by desktop, iOS and the web (Wasm); on Android the screenshot tests cover the look.
        val pixelTest = create("pixelTest") { dependsOn(uiTest) }

        // The tests that move a mouse pointer, for the targets that have one: desktop and the web (Wasm).
        val mouseTest = create("mouseTest") { dependsOn(pixelTest) }

        // The JavaScript target is left out: in its test page the drawing engine is not loaded, so every
        // test that draws fails there. It runs the common tests only; the Wasm target covers the web.
        jvmTest { dependsOn(mouseTest) }
        iosTest { dependsOn(pixelTest) }
        wasmJsTest { dependsOn(mouseTest) }

        getByName("androidHostTest") {
            dependsOn(uiTest)

            dependencies {
                implementation(libs.robolectric)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
            }
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

/** The lowest share of covered lines the build accepts, in percent. */
val minimumLineCoveragePercent = 95

// Test coverage of the library, measured on the desktop and Android host test runs together.
// `./gradlew :holler:koverHtmlReport` writes a browsable report; `koverVerify` (part of `check`, so of
// `build`) fails when fewer than 95% of the lines are covered.
kover {
    reports {
        verify {
            rule {
                minBound(minimumLineCoveragePercent)
            }
        }
    }
}

/** The license and the attribution notice, which every published file of the library carries. */
val legalFiles = rootProject.files("LICENSE", "NOTICE")

// The license asks anyone who redistributes the library to pass on its NOTICE, which only binds when the
// NOTICE is part of what they received. So both files go into every jar the library publishes (classes,
// sources and documentation, for every target) and into the Android archive. The Kotlin library files
// of the iOS and web targets (.klib) have a fixed layout and are left alone: those targets carry the
// two files in their sources jar.
tasks.withType<Jar>().configureEach {
    val isLibraryJar = name == "jvmJar" || name.endsWith("SourcesJar", ignoreCase = true) || name.endsWith("JavadocJar")

    if (isLibraryJar) from(legalFiles) { into("META-INF") }
}

tasks.withType<Zip>().matching { it.name == "bundleAndroidMainAar" }.configureEach {
    from(legalFiles)
}

// Coordinates and POM details come from the GROUP / POM_* / VERSION_NAME values in gradle.properties.
mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured, so publishing to the local Maven repository needs no key.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}
