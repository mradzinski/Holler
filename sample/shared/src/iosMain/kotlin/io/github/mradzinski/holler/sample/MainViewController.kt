package io.github.mradzinski.holler.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Creates the view controller that hosts the shared app. Called from the Swift app.
 *
 * @return a UIKit view controller drawing the app.
 */
// PascalCase on purpose: Compose Multiplatform's convention for the factory Swift calls to get the view controller.
@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
