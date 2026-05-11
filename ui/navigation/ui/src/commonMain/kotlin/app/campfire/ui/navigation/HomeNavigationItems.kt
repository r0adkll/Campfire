package app.campfire.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import app.campfire.common.compose.icons.filled.Author
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.filled.Playlists
import app.campfire.common.compose.icons.filled.Series
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Download
import app.campfire.common.compose.icons.outline.FormatListBulleted
import app.campfire.common.compose.icons.outline.Home
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Playlists
import app.campfire.common.compose.icons.outline.Podcasts
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.playlists.api.screen.PlaylistsScreen
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.nav_authors_content_description
import campfire.ui.navigation.ui.generated.resources.nav_authors_label
import campfire.ui.navigation.ui.generated.resources.nav_home_content_description
import campfire.ui.navigation.ui.generated.resources.nav_home_label
import campfire.ui.navigation.ui.generated.resources.nav_latest_content_description
import campfire.ui.navigation.ui.generated.resources.nav_latest_label
import campfire.ui.navigation.ui.generated.resources.nav_library_content_description
import campfire.ui.navigation.ui.generated.resources.nav_library_label
import campfire.ui.navigation.ui.generated.resources.nav_playlists_content_description
import campfire.ui.navigation.ui.generated.resources.nav_playlists_label
import campfire.ui.navigation.ui.generated.resources.nav_queue_content_description
import campfire.ui.navigation.ui.generated.resources.nav_queue_label
import campfire.ui.navigation.ui.generated.resources.nav_series_content_description
import campfire.ui.navigation.ui.generated.resources.nav_series_label
import campfire.ui.navigation.ui.generated.resources.nav_shows_content_description
import campfire.ui.navigation.ui.generated.resources.nav_shows_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun buildBookLibraryNavigationItems(): List<HomeNavigationItem> {
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
      screen = AuthorsScreen,
      label = stringResource(Res.string.nav_authors_label),
      contentDescription = stringResource(Res.string.nav_authors_content_description),
      iconImageVector = Icons.Outlined.Author,
      selectedImageVector = Icons.Filled.Author,
    ),
    HomeNavigationItem(
      screen = PlaylistsScreen,
      label = stringResource(Res.string.nav_playlists_label),
      contentDescription = stringResource(Res.string.nav_playlists_content_description),
      iconImageVector = Icons.Outlined.Playlists,
      selectedImageVector = Icons.Filled.Playlists,
    ),
  )
}

@Composable
internal fun buildPodcastLibraryNavigationItems(): List<HomeNavigationItem> {
  return listOf(
    HomeNavigationItem(
      screen = HomeScreen,
      label = stringResource(Res.string.nav_home_label),
      contentDescription = stringResource(Res.string.nav_home_content_description),
      iconImageVector = Icons.Outlined.Home,
      selectedImageVector = Icons.Filled.Home,
    ),
    HomeNavigationItem(
      screen = LatestEpisodesScreen,
      label = stringResource(Res.string.nav_latest_label),
      contentDescription = stringResource(Res.string.nav_latest_content_description),
      iconImageVector = Icons.Outlined.FormatListBulleted,
      selectedImageVector = Icons.Outlined.FormatListBulleted,
    ),
    HomeNavigationItem(
      screen = LibraryScreen(),
      label = stringResource(Res.string.nav_shows_label),
      contentDescription = stringResource(Res.string.nav_shows_content_description),
      iconImageVector = Icons.Outlined.Podcasts,
      selectedImageVector = Icons.Outlined.Podcasts,
    ),
    HomeNavigationItem(
      screen = AuthorsScreen, // TODO: PodcastDownload Screen
      label = stringResource(Res.string.nav_queue_label),
      contentDescription = stringResource(Res.string.nav_queue_content_description),
      iconImageVector = Icons.Outlined.Download,
      selectedImageVector = Icons.Outlined.Download,
    ),
    HomeNavigationItem(
      screen = PlaylistsScreen,
      label = stringResource(Res.string.nav_playlists_label),
      contentDescription = stringResource(Res.string.nav_playlists_content_description),
      iconImageVector = Icons.Outlined.Playlists,
      selectedImageVector = Icons.Filled.Playlists,
    ),
  )
}
