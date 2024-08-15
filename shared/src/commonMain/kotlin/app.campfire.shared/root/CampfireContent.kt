package app.campfire.shared.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.extensions.shouldUseDynamicColors
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.settings.CampfireSettings
import app.campfire.shared.di.SharedAppComponent
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
  circuit: Circuit,
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
    CircuitCompositionLocals(circuit) {
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

@Composable
fun UserComponentHome(
  campfireSettings: CampfireSettings,
) {
  val currentServerUrl by campfireSettings.observeCurrentServerUrl()
    .collectAsState(null)

  val userComponent = remember(currentServerUrl) {

  }
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
  circuit: Circuit,
  settings: CampfireSettings,
  @Assisted modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    backstack = backstack,
    navigator = navigator,
    circuit = circuit,
    settings = settings,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars.exclude(WindowInsets.statusBars),
    modifier = modifier,
  )
}
