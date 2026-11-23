import SwiftUI
import app_ios

class AppDelegate: UIResponder, UIApplicationDelegate {
    lazy var applicationComponent: IosApplicationComponent = createApplicationComponent(
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

    // iOS relaunches or wakes the app to hand over downloads that finished in the background
    func application(
        _: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        _ = BackgroundDownloadsKt.handleEventsForBackgroundURLSession(
            identifier: identifier,
            completionHandler: completionHandler
        )
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
) -> IosApplicationComponent {
    var component = IosApplicationComponentKt.createIosApplicationComponent()
    IosComponentHolder().addComponent(component: component)
    return component
}

private func createHomeUiControllerComponent(
    applicationComponent: IosApplicationComponent
) -> HomeUiControllerComponent {
    var component = applicationComponent.createHomeUiControllerComponent()
    IosComponentHolder().addComponent(component: component)
    return component
}
