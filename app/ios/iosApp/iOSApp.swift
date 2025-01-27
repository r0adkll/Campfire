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
			let uiComponent = createHomeUiControllerComponent(
                applicationComponent: delegate.applicationComponent
            )
            ContentView(component: uiComponent)
		}
	}
}

private func createApplicationComponent(
    appDelegate: AppDelegate
) -> MergedIosApplicationComponent {
    var component = IosApplicationComponent.companion.createIosApplicationComponent()
    IosComponentHolder().addComponent(component: component)
    return component
}

private func createHomeUiControllerComponent(
    applicationComponent: MergedIosApplicationComponent
) -> HomeUiControllerComponent {
    var component = applicationComponent.createHomeUiControllerComponent()
    IosComponentHolder().addComponent(component: component)
    return component
}
