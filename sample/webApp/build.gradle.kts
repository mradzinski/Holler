import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

// Web entry point of the sample, for both JS and Wasm. Its code lives in the shared webMain source set.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    js {
        browser { commonWebpackConfig { devServer = devServerOnPort8080() } }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser { commonWebpackConfig { devServer = devServerOnPort8080() } }
        binaries.executable()
    }

    sourceSets {
        webMain.dependencies {
            implementation(projects.sample.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
    }
}

/** A development server on port 8080 that does not open a browser window by itself. */
fun KotlinWebpackConfig.devServerOnPort8080(): KotlinWebpackConfig.DevServer =
    (devServer ?: KotlinWebpackConfig.DevServer()).copy(open = false, port = 8080)
