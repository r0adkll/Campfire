// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.updates.source.AppUpdate
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the drawer's update card off-screen in each state and writes PNGs to `build/renders`
 * for eyeballing; the assertions only guard that each state paints.
 */
class AppUpdateCardRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `update available`() {
    render("update-card-available") { Card(UPDATE, dismissable = true) }
  }

  @Test
  fun `update available dark`() {
    render("update-card-available-dark", dark = true) { Card(UPDATE, dismissable = true) }
  }

  @Test
  fun `downloading`() {
    render("update-card-downloading") { Card(UPDATE.copy(status = AppUpdate.Status.Downloading(0.42f))) }
  }

  @Test
  fun `ready to install`() {
    render("update-card-ready", dark = true) { Card(UPDATE.copy(status = AppUpdate.Status.ReadyToInstall)) }
  }

  @Test
  fun `failed`() {
    render("update-card-failed") { Card(UPDATE.copy(status = AppUpdate.Status.Failed), dismissable = true) }
  }

  @Test
  fun `pull request closed`() {
    render("update-card-channel-ended") {
      Card(UPDATE.copy(status = AppUpdate.Status.ChannelEnded), dismissable = true, pullRequest = 1290)
    }
  }

  @Test
  fun `pull request update`() {
    render("update-card-pull-request") { Card(UPDATE, dismissable = true, pullRequest = 1290) }
  }

  @Composable
  private fun Card(update: AppUpdate, dismissable: Boolean = false, pullRequest: Int? = null) {
    AppUpdateCard(
      update = update,
      pullRequest = pullRequest,
      onClick = {},
      onInstall = {},
      onDismiss = if (dismissable) ({}) else null,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    )
  }

  private fun render(
    name: String,
    dark: Boolean = false,
    content: @Composable () -> Unit,
  ) {
    val scene = ImageComposeScene(width = WIDTH * 2, height = HEIGHT * 2, density = Density(2f)) {
      CampfireTheme(useDarkColors = dark) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
          content()
        }
      }
    }
    scene.use {
      val image = it.render(nanoTime = 2_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    // A modal drawer sheet's width
    const val WIDTH = 360
    const val HEIGHT = 260

    val UPDATE = AppUpdate(
      versionName = "1.4.0",
      versionCode = 1_004_000L,
      headline = "Dedicated App updates settings and a fresh update card",
      sizeBytes = 48_200_000L,
      channel = "beta",
    )
  }
}
