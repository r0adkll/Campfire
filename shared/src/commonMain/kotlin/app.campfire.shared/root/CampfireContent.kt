package app.campfire.shared.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.PlatformBackHandler
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.extensions.shouldUseDynamicColors
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.logging.bark
import app.campfire.shared.navigator.OpenUrlNavigator
import com.moriatsushi.insetsx.statusBars
import com.moriatsushi.insetsx.systemBars
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.continuityRetainedStateRegistry
import com.slack.circuit.runtime.Navigator
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias CampfireContentWithInsets = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  modifier: Modifier,
) -> Unit

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Inject
@Composable
fun CampfireContentWithInsets(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  @Assisted windowInsets: WindowInsets,
  settings: CampfireSettings,
  @Assisted modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalRetainedStateRegistry provides continuityRetainedStateRegistry(),
  ) {
    UserComponentContent(
      campfireSettings = settings,
    ) { userComponent ->

      val backStack = key(userComponent.currentUserSession) { rememberSaveableBackStack(userComponent.rootScreen) }
      val navigator = key(userComponent.currentUserSession) { rememberCircuitNavigator(backStack) { onRootPop() } }

      LaunchedEffect(backStack, navigator) {
        bark {
          """
            UserComponentContent(session=${userComponent.currentUserSession})
              backStack = $backStack,
              navigator = $navigator,
            )
          """.trimIndent()
        }
      }

      PlatformBackHandler(
        enabled = backStack.size > 1,
        onBack = {
          // Check the backStack on each call as the `BackHandler` enabled state only updates on composition
          if (backStack.size > 1) {
            navigator.pop()
          }
        },
      )

      val urlNavigator: Navigator = remember(navigator) {
        OpenUrlNavigator(navigator, onOpenUrl)
      }

      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          useDarkColors = settings.shouldUseDarkColors(),
          useDynamicColors = settings.shouldUseDynamicColors(),
        ) {
          Home(
            backstack = backStack,
            navigator = urlNavigator,
            windowInsets = windowInsets,
            modifier = modifier,
          )
        }
      }
    }
  }
}

typealias CampfireContent = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun CampfireContent(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  settings: CampfireSettings,
  @Assisted modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    onRootPop = onRootPop,
    settings = settings,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars.exclude(WindowInsets.statusBars),
    modifier = modifier,
  )
}
