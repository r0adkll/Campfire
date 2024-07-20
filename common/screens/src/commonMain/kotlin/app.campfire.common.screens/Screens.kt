package app.campfire.common.screens

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen

//region App Screens

/**
 * This is a dummy screen to fill a blank detail side content since the backstack/navigator requires a
 * non-empty state to be initialized. This will just render a blank screen but will be used to automatically
 * show/hide the side detail content pain
 */
@Parcelize
class RootScreen : BaseScreen(name = "Root")

@Parcelize
class WelcomeScreen : BaseScreen(name = "Welcome()"), StaticScreen

@Parcelize
class LoginScreen : BaseScreen(name = "Login()")

@Parcelize
class SettingsScreen : BaseScreen(name = "Settings()")

//endregion

//region Utility Screens

@Parcelize
data class UrlScreen(val url: String) : BaseScreen(name = "UrlScreen()") {
  override val arguments get() = mapOf("url" to url)
}

//endregion

/**
 * The Root screen class that all other screen definitions will use as the underlying screen
 * data type
 */
abstract class BaseScreen(val name: String) : Screen {
  open val arguments: Map<String, *>? = null
  open val presentation: Presentation = Presentation()
}

/**
 * The type of presentation that a screen is. Dictating whether or not to hide the bottom nav (if available)
 * or whether this screen can be rendered as a detail screen
 */
data class Presentation(
  val hideBottomNav: Boolean = false,
  val isDetailScreen: Boolean = false,
) {
  companion object {
    val Fullscreen: Presentation get() = Presentation(hideBottomNav = true)
  }
}
