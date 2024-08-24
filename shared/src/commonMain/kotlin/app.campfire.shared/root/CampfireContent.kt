package app.campfire.shared.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.extensions.shouldUseDynamicColors
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.ComponentHolder
import app.campfire.core.session.UserSession
import app.campfire.shared.di.UserComponent
import app.campfire.shared.navigator.OpenUrlNavigator
import com.moriatsushi.insetsx.statusBars
import com.moriatsushi.insetsx.systemBars
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.continuityRetainedStateRegistry
import com.slack.circuit.runtime.Navigator
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias CampfireContentWithInsets = @Composable (
  backstack: SaveableBackStack,
  navigator: Navigator,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  modifier: Modifier,
) -> Unit

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Inject
@Composable
fun CampfireContentWithInsets(
  @Assisted backstack: SaveableBackStack,
  @Assisted navigator: Navigator,
  @Assisted onOpenUrl: (String) -> Unit,
  @Assisted windowInsets: WindowInsets,
  settings: CampfireSettings,
  @Assisted modifier: Modifier = Modifier,
) {
  val urlNavigator: Navigator = remember(navigator) {
    OpenUrlNavigator(navigator, onOpenUrl)
  }

  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalRetainedStateRegistry provides continuityRetainedStateRegistry(),
  ) {
    UserComponentContent(
      campfireSettings = settings,
    ) { userComponent ->

      // Everytime the user session changes reset the backstack to the root screen for a given user
      // session type
      LaunchedEffect(userComponent.currentUserSession) {
        backstack.popUntil { false }
        backstack.push(userComponent.rootScreen)
      }

      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          useDarkColors = settings.shouldUseDarkColors(),
          useDynamicColors = settings.shouldUseDynamicColors(),
        ) {
          Home(
            backstack = backstack,
            navigator = urlNavigator,
            windowInsets = windowInsets,
            modifier = modifier,
          )
        }
      }
    }
  }
}

@Composable
fun UserComponentContent(
  campfireSettings: CampfireSettings,
  content: @Composable (UserComponent) -> Unit,
) {
  val currentServerUrl by campfireSettings.observeCurrentServerUrl()
    .collectAsState(null)

  val userComponent = remember(currentServerUrl) {
    ComponentHolder.component<UserComponent.Factory>()
      .create(
        currentServerUrl?.let {
          UserSession.LoggedIn(it)
        } ?: UserSession.LoggedOut
      )
  }

  content(userComponent)
}

typealias CampfireContent = @Composable (
  backstack: SaveableBackStack,
  navigator: Navigator,
  onOpenUrl: (String) -> Unit,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun CampfireContent(
  @Assisted backstack: SaveableBackStack,
  @Assisted navigator: Navigator,
  @Assisted onOpenUrl: (String) -> Unit,
  settings: CampfireSettings,
  @Assisted modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    backstack = backstack,
    navigator = navigator,
    settings = settings,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars.exclude(WindowInsets.statusBars),
    modifier = modifier,
  )
}
