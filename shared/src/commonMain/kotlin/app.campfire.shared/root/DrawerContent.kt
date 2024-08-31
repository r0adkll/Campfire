package app.campfire.shared.root

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.account.ui.switcher.AccountSwitcher
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.common.screens.StorageScreen
import campfire.shared.generated.resources.Res
import campfire.shared.generated.resources.nav_home_content_description
import campfire.shared.generated.resources.nav_home_label
import campfire.shared.generated.resources.nav_settings_content_description
import campfire.shared.generated.resources.nav_settings_label
import campfire.shared.generated.resources.nav_statistics_content_description
import campfire.shared.generated.resources.nav_statistics_label
import campfire.shared.generated.resources.nav_storage_content_description
import campfire.shared.generated.resources.nav_storage_label
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

typealias DrawerContent = @Composable (
  navigator: Navigator,
  selectedNavigation: Screen,
  drawerState: DrawerState,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun DrawerContent(
  @Assisted navigator: Navigator,
  @Assisted selectedNavigation: Screen,
  @Assisted drawerState: DrawerState,
  accountSwitcher: AccountSwitcher,
  @Assisted modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val drawerItems = buildDrawerItems()
  ModalDrawerSheet(
    modifier = modifier,
  ) {
    // Show the root account switcher
    accountSwitcher(
      {},
      Modifier,
    )
    Spacer(Modifier.height(8.dp))
    for (item in drawerItems) {
      NavigationDrawerItem(
        icon = {
          Icon(
            imageVector = item.iconImageVector,
            contentDescription = item.contentDescription,
          )
        },
        label = { Text(text = item.label) },
        selected = selectedNavigation == item.screen,
        onClick = {
          navigator.resetRoot(item.screen)
          coroutineScope.launch {
            drawerState.close()
          }
        },
        modifier = Modifier
          .padding(
            horizontal = 16.dp
          )
      )
    }
  }
}

@Composable
private fun buildDrawerItems(): List<HomeNavigationItem> {
  return listOf(
    HomeNavigationItem(
      screen = HomeScreen,
      label = stringResource(Res.string.nav_home_label),
      contentDescription = stringResource(Res.string.nav_home_content_description),
      iconImageVector = Icons.Rounded.Home,
      selectedImageVector = Icons.Filled.Home,
    ),
    HomeNavigationItem(
      screen = StatisticsScreen,
      label = stringResource(Res.string.nav_statistics_label),
      contentDescription = stringResource(Res.string.nav_statistics_content_description),
      iconImageVector = Icons.Rounded.QueryStats,
      selectedImageVector = Icons.Filled.QueryStats,
    ),
    HomeNavigationItem(
      screen = StorageScreen,
      label = stringResource(Res.string.nav_storage_label),
      contentDescription = stringResource(Res.string.nav_storage_content_description),
      iconImageVector = Icons.Rounded.Folder,
      selectedImageVector = Icons.Filled.Folder,
    ),
    HomeNavigationItem(
      screen = SettingsScreen,
      label = stringResource(Res.string.nav_settings_label),
      contentDescription = stringResource(Res.string.nav_settings_content_description),
      iconImageVector = Icons.Rounded.Settings,
      selectedImageVector = Icons.Filled.Settings,
    ),
  )
}
