import SwiftUI
import UIKit
import AdzanNotifShared

private struct SharedComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosPlatformKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct AdzanNotifApp: App {
    var body: some Scene {
        WindowGroup {
            SharedComposeView()
                .ignoresSafeArea()
        }
    }
}
