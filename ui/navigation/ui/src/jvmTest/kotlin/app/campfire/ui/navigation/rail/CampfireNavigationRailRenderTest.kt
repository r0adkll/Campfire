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
 * Renders the compact navigation rail off-screen at a tablet height and at the height of a
 * half-open foldable's upper half, and writes PNGs to `build/renders` for eyeballing. The short
 * render checks that a full destination list neither squashes nor runs off the end: it scrolls
 * between the pinned header and Settings item.
 */
@OptIn(ExperimentalComposeUiApi::class)
class CampfireNavigationRailRenderTest {

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
  )

  @Test
  fun `tall rail shows every destination`() {
    render("rail-tall", height = 900) {
      Rail()
    }
  }

  @Test
  fun `a foldable's upper half scrolls the destinations between the pinned header and settings`() {
    render("rail-short", height = 350) {
      Rail()
    }
  }

  @Composable
  private fun Rail() {
    CampfireTheme(useDarkColors = false) {
      Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CampfireNavigationRailContent(
          navigationItems = items,
          selectedNavigation = HomeScreen,
          onNavigationSelected = {},
          header = {
            // Stand-in for the server icon
            Box(
              Modifier
                .size(40.dp)
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
    const val WIDTH = 160
  }
}
