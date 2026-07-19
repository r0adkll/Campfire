package app.campfire.ui.navigation.rail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.NavigationComponent
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.settings
import campfire.ui.navigation.ui.generated.resources.settings_content_description
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.stringResource

private val ServerIconSizeSmall = 40.dp
private val ServerIconSizeLarge = 56.dp

@Composable
fun CampfireNavigationRail(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  onMenuSelected: () -> Unit,
  modifier: Modifier = Modifier,
  navigationComponent: NavigationComponent = rememberComponent(),
) {
  val windowSizeClass = LocalWindowSizeClass.current
  val presenter = remember(navigationComponent) {
    navigationComponent.navigationPresenterFactory()
  }
  val navigationItems = presenter.present()

  NavigationRail(
    modifier = modifier,
    header = {
      ServerIcon(
        onClick = onMenuSelected,
        size = if (windowSizeClass.isLandscapePhone) ServerIconSizeSmall else ServerIconSizeLarge,
      )
    },
  ) {
    for (item in navigationItems) {
      NavigationRailItem(
        icon = {
          HomeNavigationItemIcon(
            item = item,
            selected = item.screen.instanceOf(selectedNavigation::class),
          )
        },
        alwaysShowLabel = false,
        label = { Text(text = item.label) },
        selected = selectedNavigation == item.screen,
        onClick = { onNavigationSelected(item.screen) },
      )
    }

    Spacer(Modifier.weight(1f))
    NavigationRailItem(
      icon = {
        Icon(
          imageVector = Icons.Outlined.Settings,
          contentDescription = stringResource(Res.string.settings_content_description),
        )
      },
      label = { Text(text = stringResource(Res.string.settings)) },
      selected = false,
      onClick = { onNavigationSelected(SettingsScreen()) },
    )
  }
}
