// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.CloudDownload
import app.campfire.common.compose.icons.filled.Collections
import app.campfire.common.compose.icons.filled.Event
import app.campfire.common.compose.icons.filled.QueryStats
import app.campfire.common.compose.icons.filled.Settings
import app.campfire.common.compose.icons.outline.Collections
import app.campfire.common.compose.icons.rounded.CloudDownload
import app.campfire.common.compose.icons.rounded.Event
import app.campfire.common.compose.icons.rounded.QueryStats
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.next
import app.campfire.core.model.MediaType
import app.campfire.discover.api.screen.UpcomingScreen
import app.campfire.libraries.api.LibraryRepository
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.navigation.drawer.DrawerUiEvent
import app.campfire.ui.navigation.drawer.DrawerUiState
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.nav_collections_content_description
import campfire.ui.navigation.ui.generated.resources.nav_collections_label
import campfire.ui.navigation.ui.generated.resources.nav_downloads_content_description
import campfire.ui.navigation.ui.generated.resources.nav_downloads_label
import campfire.ui.navigation.ui.generated.resources.nav_settings_content_description
import campfire.ui.navigation.ui.generated.resources.nav_settings_label
import campfire.ui.navigation.ui.generated.resources.nav_statistics_content_description
import campfire.ui.navigation.ui.generated.resources.nav_statistics_label
import campfire.ui.navigation.ui.generated.resources.nav_upcoming_content_description
import campfire.ui.navigation.ui.generated.resources.nav_upcoming_label
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

typealias NavigationPresenterFactory = () -> NavigationPresenter

@ContributesTo(UserScope::class)
interface NavigationComponent {
  val navigationPresenterFactory: NavigationPresenterFactory
}

@Inject
class NavigationPresenter(
  private val libraryRepository: LibraryRepository,
  private val remoteEpisodeDownloadTracker: RemoteEpisodeDownloadTracker,
  private val settings: CampfireSettings,
) {

  @Composable
  fun present(): List<HomeNavigationItem> {
    val currentLibrary by remember {
      libraryRepository.observeCurrentLibrary()
    }.collectAsState(null)
    val downloadQueueCount by remember {
      remoteEpisodeDownloadTracker.state
    }.collectAsState()

    return when (currentLibrary?.mediaType) {
      MediaType.Podcast -> buildPodcastLibraryNavigationItems(
        downloadQueueCount = downloadQueueCount.values.sumOf { it.size },
      )
      else -> buildBookLibraryNavigationItems()
    }
  }

  @Composable
  fun presentDrawer(): DrawerUiState {
    val items = buildList {
      val currentLibrary by remember {
        libraryRepository.observeCurrentLibrary()
      }.collectAsState(null)
      val downloadQueueCount by remember {
        remoteEpisodeDownloadTracker.state
      }.collectAsState()

      val navigationType = LocalWindowSizeClass.current.navigationType
      if (navigationType == NavigationType.Drawer) {
        addAll(
          when (currentLibrary?.mediaType) {
            MediaType.Podcast -> buildPodcastLibraryNavigationItems(
              downloadQueueCount = downloadQueueCount.values.sumOf { it.size },
            )
            else -> buildBookLibraryNavigationItems()
          },
        )
      }

      if (currentLibrary?.mediaType != MediaType.Podcast) {
        add(
          HomeNavigationItem(
            screen = CollectionsScreen,
            label = stringResource(Res.string.nav_collections_label),
            contentDescription = stringResource(Res.string.nav_collections_content_description),
            iconImageVector = CampfireIcons.Outline.Collections,
            selectedImageVector = CampfireIcons.Filled.Collections,
          ),
        )

        add(
          HomeNavigationItem(
            screen = UpcomingScreen,
            label = stringResource(Res.string.nav_upcoming_label),
            contentDescription = stringResource(Res.string.nav_upcoming_content_description),
            iconImageVector = CampfireIcons.Rounded.Event,
            selectedImageVector = CampfireIcons.Filled.Event,
          ),
        )
      }

      add(
        HomeNavigationItem(
          screen = StatisticsScreen,
          label = stringResource(Res.string.nav_statistics_label),
          contentDescription = stringResource(Res.string.nav_statistics_content_description),
          iconImageVector = CampfireIcons.Rounded.QueryStats,
          selectedImageVector = CampfireIcons.Filled.QueryStats,
        ),
      )

      add(
        HomeNavigationItem(
          screen = SettingsScreen(SettingsScreen.Page.Downloads),
          label = stringResource(Res.string.nav_downloads_label),
          contentDescription = stringResource(Res.string.nav_downloads_content_description),
          iconImageVector = CampfireIcons.Rounded.CloudDownload,
          selectedImageVector = CampfireIcons.Filled.CloudDownload,
        ),
      )

      add(
        HomeNavigationItem(
          screen = SettingsScreen(),
          label = stringResource(Res.string.nav_settings_label),
          contentDescription = stringResource(Res.string.nav_settings_content_description),
          iconImageVector = CampfireIcons.Rounded.Settings,
          selectedImageVector = CampfireIcons.Filled.Settings,
        ),
      )
    }

    val themeMode by remember {
      settings.observeTheme()
    }.collectAsState()

    return DrawerUiState(
      themeMode = themeMode,
      navigationItems = items,
    ) { event ->
      when (event) {
        DrawerUiEvent.CycleThemeMode -> {
          settings.themeMode = themeMode.next()
        }
      }
    }
  }
}
