package app.campfire.common.navigator

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.EmptyScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.popUntil
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

class HomeNavigator(
  private val windowSizeClass: WindowSizeClass,
  private val rootNavigator: Navigator,
  private val detailNavigator: Navigator,
) : Navigator {

  private val isSupportingPaneEnabled by lazy { windowSizeClass.isSupportingPaneEnabled }

  override fun goTo(screen: Screen): Boolean {
    return if (isSupportingPaneEnabled && screen is DetailScreen) {
      detailNavigator.popUntil { it is EmptyScreen }
      detailNavigator.goTo(screen)
    } else {
      rootNavigator.goTo(screen)
    }
  }

  override fun peek(): Screen? = rootNavigator.peek()

  override fun peekBackStack(): ImmutableList<Screen> = rootNavigator.peekBackStack()

  override fun pop(result: PopResult?): Screen? = rootNavigator.pop()

  override fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean): ImmutableList<Screen> {
    return if (isSupportingPaneEnabled && newRoot is DetailScreen) {
      detailNavigator.resetRoot(newRoot, saveState, restoreState)
    } else {
      rootNavigator.resetRoot(newRoot, saveState, restoreState)
    }
  }
}
