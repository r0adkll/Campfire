package app.campfire.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.filled.Author
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.filled.Playlists
import app.campfire.common.compose.icons.filled.Series
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Home
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Playlists
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.playlists.api.screen.PlaylistsScreen
import campfire.ui.navigation.generated.resources.Res
import campfire.ui.navigation.generated.resources.nav_authors_content_description
import campfire.ui.navigation.generated.resources.nav_authors_label
import campfire.ui.navigation.generated.resources.nav_home_content_description
import campfire.ui.navigation.generated.resources.nav_home_label
import campfire.ui.navigation.generated.resources.nav_library_content_description
import campfire.ui.navigation.generated.resources.nav_library_label
import campfire.ui.navigation.generated.resources.nav_playlists_content_description
import campfire.ui.navigation.generated.resources.nav_playlists_label
import campfire.ui.navigation.generated.resources.nav_series_content_description
import campfire.ui.navigation.generated.resources.nav_series_label
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.stringResource

@Immutable
data class HomeNavigationItem(
  val screen: Screen,
  val label: String,
  val contentDescription: String,
  val iconImageVector: ImageVector,
  val selectedImageVector: ImageVector? = null,
)

@Composable
internal fun buildNavigationItems(): List<HomeNavigationItem> {
  return listOf(
    HomeNavigationItem(
      screen = HomeScreen,
      label = stringResource(Res.string.nav_home_label),
      contentDescription = stringResource(Res.string.nav_home_content_description),
      iconImageVector = Icons.Outlined.Home,
      selectedImageVector = Icons.Filled.Home,
    ),
    HomeNavigationItem(
      screen = LibraryScreen(),
      label = stringResource(Res.string.nav_library_label),
      contentDescription = stringResource(Res.string.nav_library_content_description),
      iconImageVector = Icons.Outlined.Library,
      selectedImageVector = Icons.Filled.Library,
    ),
    HomeNavigationItem(
      screen = SeriesScreen,
      label = stringResource(Res.string.nav_series_label),
      contentDescription = stringResource(Res.string.nav_series_content_description),
      iconImageVector = Icons.Outlined.Series,
      selectedImageVector = Icons.Filled.Series,
    ),
    HomeNavigationItem(
      screen = PlaylistsScreen,
      label = stringResource(Res.string.nav_playlists_label),
      contentDescription = stringResource(Res.string.nav_playlists_content_description),
      iconImageVector = Icons.Outlined.Playlists,
      selectedImageVector = Icons.Filled.Playlists,
    ),
    HomeNavigationItem(
      screen = AuthorsScreen,
      label = stringResource(Res.string.nav_authors_label),
      contentDescription = stringResource(Res.string.nav_authors_content_description),
      iconImageVector = Icons.Outlined.Author,
      selectedImageVector = Icons.Filled.Author,
    ),
  )
}
