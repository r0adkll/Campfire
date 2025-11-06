package app.campfire.common.navigator

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.screens.BaseScreen
import app.campfire.crashreporting.CrashReporter
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationEventListener

class AnalyticsNavigationEventListener(
  private val analytics: Analytics,
  private val crashReporter: CrashReporter,
) : NavigationEventListener {

  override fun onBackStackChanged(backStack: List<Screen>, navigationContext: NavigationContext) {
    crashReporter.tag("backstack", backStack.joinToString { it.analyticsName() ?: "" })
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext) {
    sendScreenView(screen)
  }

  override fun pop(result: PopResult?, navigationContext: NavigationContext) {
    navigationContext.peek()?.let { screen ->
      sendScreenView(screen)
    }
  }

  override fun resetRoot(newRoot: Screen, options: Navigator.StateOptions, navigationContext: NavigationContext) {
    sendScreenView(newRoot)
  }

  private fun sendScreenView(screen: Screen) {
    analytics.send(
      ScreenViewEvent(
        screen.analyticsName() ?: "unknown",
      ),
    )
  }
}

internal fun Screen.analyticsName() = if (this is BaseScreen) {
  name
} else {
  this::class.simpleName
}
