// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.rail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Collections
import app.campfire.common.compose.icons.outline.Home
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Playlists
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.compose.icons.rounded.CloudDownload
import app.campfire.common.compose.icons.rounded.Event
import app.campfire.common.compose.icons.rounded.QueryStats
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.discover.api.screen.UpcomingScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.playlists.api.screen.PlaylistsScreen
import app.campfire.ui.navigation.HomeNavigationItem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the desktop wide navigation rail off-screen in its collapsed and expanded states and
 * writes PNGs to `build/renders` for eyeballing; the assertions only guard that each state
 * composes and paints. The short render checks that a full book-library destination list still
 * fits (by scrolling) in a window shorter than the list.
 */
@OptIn(ExperimentalComposeUiApi::class)
class CampfireWideNavigationRailRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val items = listOf(
    HomeNavigationItem(HomeScreen, "Home", "Home", CampfireIcons.Outline.Home, CampfireIcons.Filled.Home),
    HomeNavigationItem(LibraryScreen(), "Library", "Library", CampfireIcons.Outline.Library),
    HomeNavigationItem(SeriesScreen, "Series", "Series", CampfireIcons.Outline.Series),
    HomeNavigationItem(AuthorsScreen, "Authors", "Authors", CampfireIcons.Outline.Author),
    HomeNavigationItem(PlaylistsScreen, "Playlists", "Playlists", CampfireIcons.Outline.Playlists),
    HomeNavigationItem(CollectionsScreen, "Collections", "Collections", CampfireIcons.Outline.Collections),
    HomeNavigationItem(UpcomingScreen, "Upcoming", "Upcoming", CampfireIcons.Rounded.Event),
    HomeNavigationItem(StatisticsScreen, "Statistics", "Statistics", CampfireIcons.Rounded.QueryStats),
    HomeNavigationItem(
      SettingsScreen(SettingsScreen.Page.Downloads),
      "Downloads",
      "Downloads",
      CampfireIcons.Rounded.CloudDownload,
      badgeCount = 3,
    ),
    HomeNavigationItem(SettingsScreen(), "Settings", "Settings", CampfireIcons.Rounded.Settings),
  )

  @Test
  fun `collapsed rail`() {
    render("wide-rail-collapsed", height = 900) {
      Rail(expanded = false)
    }
  }

  @Test
  fun `expanded rail`() {
    render("wide-rail-expanded", height = 900) {
      Rail(expanded = true)
    }
  }

  @Test
  fun `collapsed rail in a short window scrolls its destinations`() {
    render("wide-rail-collapsed-short", height = 600) {
      Rail(expanded = false)
    }
  }

  @Composable
  private fun Rail(expanded: Boolean) {
    CampfireTheme(useDarkColors = false) {
      Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CampfireWideNavigationRailContent(
          state = WideNavigationRailUiState(
            navigationItems = items,
            expanded = expanded,
            eventSink = {},
          ),
          selectedNavigation = HomeScreen,
          onNavigationSelected = {},
          accountContent = {
            // Stand-in for the account switcher: an icon collapsed, a card expanded
            Box(
              Modifier
                .size(width = if (expanded) 232.dp else 56.dp, height = if (expanded) 120.dp else 56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
            )
          },
          modifier = Modifier.fillMaxHeight(),
        )
      }
    }
  }

  private fun render(name: String, height: Int, content: @Composable () -> Unit) {
    ImageComposeScene(width = WIDTH * 2, height = height * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      val image = scene.render(nanoTime = 1_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    const val WIDTH = 420
  }
}
