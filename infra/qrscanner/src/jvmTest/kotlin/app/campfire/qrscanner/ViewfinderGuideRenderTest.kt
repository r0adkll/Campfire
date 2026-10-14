// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the guide over a stand-in for the camera's view and writes PNGs to `build/renders` for
 * eyeballing. The assertion only guards that it composes and paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ViewfinderGuideRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `waiting for a code`() {
    render("viewfinder-guide-waiting", captured = false)
  }

  @Test
  fun `a code was read`() {
    render("viewfinder-guide-captured", captured = true)
  }

  private fun render(name: String, captured: Boolean) {
    render(name) {
      CampfireTheme(useDarkColors = false) {
        Box(
          Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF7A8C6E), Color(0xFFC9B79C), Color(0xFF4E5D6C)))),
        ) {
          ViewfinderGuide(
            captured = captured,
            hint = {
              Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                Text("Point your camera at the setup QR code", Modifier.padding(16.dp))
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }

  private fun render(name: String, content: @Composable () -> Unit) {
    ImageComposeScene(width = 400 * 2, height = 860 * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      val image = scene.render(nanoTime = 1_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      assertThat(bytes.size).isGreaterThan(1_000)
    }
  }
}
