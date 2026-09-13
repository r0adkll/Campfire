// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.audioplayer.AudioDevice
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.sessions.ui.composables.OutputDeviceMenuItems
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The device rows themselves, which are what both the standalone picker and the bar's overflow
 * menu show. The overflow path is the one reached at ordinary desktop widths, where the picker
 * folds away, and a menu cannot be opened inside an [ImageComposeScene] — so the shared content
 * is rendered directly instead.
 */
@OptIn(ExperimentalComposeUiApi::class)
class OutputDeviceMenuRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val devices = listOf(
    AudioDevice("MacBook Pro Speakers", "MacBook Pro Speakers"),
    AudioDevice("External Headphones", "External Headphones"),
  )

  @Test
  fun `following the system default`() {
    render("device-menu-default") {
      Menu(OutputDeviceUiState(devices = devices, selectedName = null, selectedIsMissing = false) {})
    }
  }

  @Test
  fun `a pinned device is ticked`() {
    render("device-menu-pinned") {
      Menu(
        OutputDeviceUiState(
          devices = devices,
          selectedName = "External Headphones",
          selectedIsMissing = false,
        ) {},
      )
    }
  }

  @Test
  fun `a pinned device that was unplugged still shows, disabled`() {
    render("device-menu-missing") {
      Menu(
        OutputDeviceUiState(
          devices = listOf(devices.first()),
          selectedName = "External Headphones",
          selectedIsMissing = true,
        ) {},
      )
    }
  }

  @Composable
  private fun Menu(state: OutputDeviceUiState) {
    CampfireTheme(useDarkColors = false) {
      Surface(Modifier.fillMaxSize()) {
        Column {
          OutputDeviceMenuItems(state, onChosen = {})
        }
      }
    }
  }

  private fun render(name: String, content: @Composable () -> Unit) {
    ImageComposeScene(width = WIDTH * 2, height = HEIGHT * 2, density = Density(2f), content = content)
      .use { scene ->
        val image = scene.render(nanoTime = 1_000_000_000L)
        val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
        File(renders, "$name.png").writeBytes(bytes)
        println("rendered ${File(renders, "$name.png").absolutePath}")
        assertThat(bytes.size).isGreaterThan(1_000)
      }
  }

  private companion object {
    const val WIDTH = 320
    const val HEIGHT = 220
  }
}
