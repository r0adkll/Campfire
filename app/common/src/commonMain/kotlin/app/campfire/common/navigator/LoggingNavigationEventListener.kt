package app.campfire.common.navigator

import app.campfire.core.logging.Cork
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import kotlinx.collections.immutable.ImmutableList

object LoggingNavigationEventListener : NavigationEventListener, Cork {

  override val tag: String = "Navigation"

  override fun goTo(screen: Screen) {
    ibark { "goTo(${screen.analyticsName() ?: "<unknown>"})" }
  }

  override fun pop(
    backStack: ImmutableList<Screen>,
    result: PopResult?,
  ) {
    val backStackReadable = backStack.joinToString(
      prefix = "[",
      postfix = "]",
    ) { it.analyticsName() ?: "<unknown>" }
    ibark { "pop(backstack=$backStackReadable, result=$result)" }
  }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ) {
    ibark { "resetRoot(${newRoot.analyticsName() ?: "<unknown>"}, saveState=$saveState, restoreState=$restoreState)" }
  }
}
