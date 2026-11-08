// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CloudDownload
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.screens.DownloadsScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.ui.navigation.HomeNavigationItem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders drawer destinations with a download count, an app-update dot and downloads in progress,
 * caught mid-sweep, and writes PNGs to `build/renders` for eyeballing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DestinationListItemRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `drawer destinations light`() {
    render("drawer-destinations-light", dark = false)
  }

  @Test
  fun `drawer destinations dark`() {
    render("drawer-destinations-dark", dark = true)
  }

  @Composable
  private fun Destinations(dark: Boolean) {
    CampfireTheme(useDarkColors = dark) {
      Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = 8.dp)) {
          DestinationListItem(item = downloads(count = 12, inProgress = false), selected = false, onClick = {})
          DestinationListItem(item = downloads(count = 3, inProgress = true), selected = false, onClick = {})
          DestinationListItem(item = downloads(count = 1_200, inProgress = true), selected = true, onClick = {})
          DestinationListItem(
            item = HomeNavigationItem(
              screen = SettingsScreen(),
              label = "Settings",
              contentDescription = "Settings",
              iconImageVector = CampfireIcons.Rounded.Settings,
              badged = true,
            ),
            selected = false,
            onClick = {},
          )
        }
      }
    }
  }

  private fun downloads(count: Int, inProgress: Boolean) = HomeNavigationItem(
    screen = DownloadsScreen,
    label = "Downloads",
    contentDescription = "Downloads",
    iconImageVector = CampfireIcons.Rounded.CloudDownload,
    badgeCount = count,
    inProgress = inProgress,
  )

  private fun render(name: String, dark: Boolean) {
    ImageComposeScene(width = WIDTH * 2, height = HEIGHT * 2, density = Density(2f)) {
      Destinations(dark)
    }.use { scene ->
      // Step through the fade-in to partway down the sweep
      for (frame in 0..12) scene.render(nanoTime = frame * 50_000_000L)
      val image = scene.render(nanoTime = 700_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    const val WIDTH = 320
    const val HEIGHT = 248
  }
}
