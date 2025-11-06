package app.campfire.common.navigator

import app.campfire.core.logging.Cork
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationEventListener

object LoggingNavigationEventListener : NavigationEventListener, Cork {

  override val tag: String = "Navigation"

  override fun goTo(screen: Screen, navigationContext: NavigationContext) {
    ibark { "goTo(${screen.analyticsName() ?: "<unknown>"})" }
  }

  override fun pop(result: PopResult?, navigationContext: NavigationContext) {
    val backStackReadable = navigationContext.peekBackStack()?.joinToString(
      prefix = "[",
      postfix = "]",
    ) { it.analyticsName() ?: "<unknown>" }
    ibark { "pop(backstack=$backStackReadable, result=$result)" }
  }

  override fun resetRoot(newRoot: Screen, options: Navigator.StateOptions, navigationContext: NavigationContext) {
    ibark { "resetRoot(${newRoot.analyticsName() ?: "<unknown>"}, options=$options)" }
  }
}
