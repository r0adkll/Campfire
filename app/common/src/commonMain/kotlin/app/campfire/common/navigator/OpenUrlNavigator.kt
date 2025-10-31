package app.campfire.common.navigator

import app.campfire.common.screens.UrlScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

class OpenUrlNavigator(
  private val navigator: Navigator,
  private val onOpenUrl: (String) -> Unit,
) : Navigator by navigator {

  override fun goTo(screen: Screen): Boolean {
    return when (screen) {
      is UrlScreen -> {
        onOpenUrl(screen.url)
        true
      }
      else -> navigator.goTo(screen)
    }
  }
}
