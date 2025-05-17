import SwiftUI
import app_ios

class AppDelegate: UIResponder, UIApplicationDelegate {
    lazy var applicationComponent: MergedIosApplicationComponent = createApplicationComponent(
        appDelegate: self
    )

    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
//        if !(FirebaseOptions.defaultOptions()?.apiKey?.isEmpty ?? true) {
//            FirebaseApp.configure()
//        }
        applicationComponent.startupInitializer.initialize()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

	var body: some Scene {
		WindowGroup {
			let uiComponent = createHomeUiControllerComponent()
            ContentView(component: uiComponent)
		}
	}
}

private func createApplicationComponent(
    appDelegate: AppDelegate
) -> IosApplicationComponent {
    return IosDI.createApplicationComponent()
}

private func createHomeUiControllerComponent() -> HomeUiControllerComponent {
    return IosDI.createHomeUiControllerComponent()
}
