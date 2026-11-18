import SwiftUI
import SampleShared

/// The iOS sample app: one window showing the shared Compose app.
@main
struct iOSApp: App {
    /// Starts the shared module's dependency injection, once, before the first screen is drawn.
    init() {
        StartSampleKt.startSample()
    }

    /// The single window of the app, filled edge to edge by the Compose screen.
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

/// Wraps the Compose view controller so SwiftUI can show it.
struct ComposeView: UIViewControllerRepresentable {
    /// Creates the Compose view controller exported by the shared Kotlin module.
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    /// Nothing to update: the Compose screen manages its own state.
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
