package app.campfire.ui.drawer

import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.account.ui.picker.AccountPickerResult
import app.campfire.account.ui.picker.showAccountPicker
import app.campfire.account.ui.switcher.AccountSwitcher
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.filled.Author
import app.campfire.common.compose.icons.filled.Collections
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.filled.Series
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Collections
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.navigation.LocalDrawerState
import app.campfire.common.compose.navigation.LocalRootScreen
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.LibraryScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.common.screens.StorageScreen
import app.campfire.core.di.UserScope
import campfire.ui.drawer.generated.resources.Res
import campfire.ui.drawer.generated.resources.nav_authors_content_description
import campfire.ui.drawer.generated.resources.nav_authors_label
import campfire.ui.drawer.generated.resources.nav_collections_content_description
import campfire.ui.drawer.generated.resources.nav_collections_label
import campfire.ui.drawer.generated.resources.nav_home_content_description
import campfire.ui.drawer.generated.resources.nav_home_label
import campfire.ui.drawer.generated.resources.nav_library_content_description
import campfire.ui.drawer.generated.resources.nav_library_label
import campfire.ui.drawer.generated.resources.nav_series_content_description
import campfire.ui.drawer.generated.resources.nav_series_label
import campfire.ui.drawer.generated.resources.nav_settings_content_description
import campfire.ui.drawer.generated.resources.nav_settings_label
import campfire.ui.drawer.generated.resources.nav_statistics_content_description
import campfire.ui.drawer.generated.resources.nav_statistics_label
import campfire.ui.drawer.generated.resources.nav_storage_content_description
import campfire.ui.drawer.generated.resources.nav_storage_label
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@CircuitInject(DrawerScreen::class, UserScope::class)
@Composable
fun Drawer(
  state: DrawerUiState,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val drawerItems = buildDrawerItems()
  val drawerState by rememberUpdatedState(LocalDrawerState.current)
  val rootScreen by rememberUpdatedState(LocalRootScreen.current)
  val overlayHost = LocalOverlayHost.current

  DrawerSheet(
    modifier = modifier,
  ) {
    // Show the root account switcher
    AccountSwitcher(
      {
        coroutineScope.launch {
          when (val result = overlayHost.showAccountPicker()) {
            AccountPickerResult.AddAccount -> state.eventSink(DrawerUiEvent.AddAccount)
            is AccountPickerResult.SwitchAccount -> state.eventSink(DrawerUiEvent.SwitchAccount(result.server))
            else -> Unit
          }
        }
      },
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
        selected = rootScreen == item.screen,
        onClick = {
          state.eventSink(DrawerUiEvent.ItemClick(item))
          coroutineScope.launch {
            drawerState?.close()
          }
        },
        modifier = Modifier
          .padding(
            horizontal = 16.dp,
          ),
      )
    }
  }
}

@Composable
private fun DrawerSheet(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val navigationType = LocalWindowSizeClass.current.navigationType
  if (navigationType == NavigationType.Drawer) {
    PermanentDrawerSheet(
      content = content,
      modifier = modifier,
    )
  } else {
    ModalDrawerSheet(
      content = content,
      modifier = modifier,
    )
  }
}

@Composable
private fun buildDrawerItems(): List<HomeNavigationItem> {
  val navigationType = LocalWindowSizeClass.current.navigationType
  return buildList {
    add(
      HomeNavigationItem(
        screen = HomeScreen,
        label = stringResource(Res.string.nav_home_label),
        contentDescription = stringResource(Res.string.nav_home_content_description),
        iconImageVector = Icons.Rounded.Home,
        selectedImageVector = Icons.Filled.Home,
      ),
    )

    if (navigationType == NavigationType.Drawer) {
      add(
        HomeNavigationItem(
          screen = LibraryScreen,
          label = stringResource(Res.string.nav_library_label),
          contentDescription = stringResource(Res.string.nav_library_content_description),
          iconImageVector = Icons.Outlined.Library,
          selectedImageVector = Icons.Filled.Library,
        ),
      )
      add(
        HomeNavigationItem(
          screen = SeriesScreen,
          label = stringResource(Res.string.nav_series_label),
          contentDescription = stringResource(Res.string.nav_series_content_description),
          iconImageVector = Icons.Outlined.Series,
          selectedImageVector = Icons.Filled.Series,
        ),
      )
      add(
        HomeNavigationItem(
          screen = CollectionsScreen,
          label = stringResource(Res.string.nav_collections_label),
          contentDescription = stringResource(Res.string.nav_collections_content_description),
          iconImageVector = Icons.Outlined.Collections,
          selectedImageVector = Icons.Filled.Collections,
        ),
      )
      add(
        HomeNavigationItem(
          screen = AuthorsScreen,
          label = stringResource(Res.string.nav_authors_label),
          contentDescription = stringResource(Res.string.nav_authors_content_description),
          iconImageVector = Icons.Outlined.Author,
          selectedImageVector = Icons.Filled.Author,
        ),
      )
    }

    add(
      HomeNavigationItem(
        screen = StatisticsScreen,
        label = stringResource(Res.string.nav_statistics_label),
        contentDescription = stringResource(Res.string.nav_statistics_content_description),
        iconImageVector = Icons.Rounded.QueryStats,
        selectedImageVector = Icons.Filled.QueryStats,
      ),
    )
    add(
      HomeNavigationItem(
        screen = StorageScreen,
        label = stringResource(Res.string.nav_storage_label),
        contentDescription = stringResource(Res.string.nav_storage_content_description),
        iconImageVector = Icons.Rounded.Folder,
        selectedImageVector = Icons.Filled.Folder,
      ),
    )
    add(
      HomeNavigationItem(
        screen = SettingsScreen,
        label = stringResource(Res.string.nav_settings_label),
        contentDescription = stringResource(Res.string.nav_settings_content_description),
        iconImageVector = Icons.Rounded.Settings,
        selectedImageVector = Icons.Filled.Settings,
      ),
    )
  }
}
