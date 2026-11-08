// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the update badge on a stand-in theme icon (with the connection dot it shares the icon
 * with) across the drop animation, writing PNGs to `build/renders` for eyeballing.
 */
class UpdateIndicatorRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `light`() = render("update-indicator-light", dark = false)

  @Test
  fun `dark`() = render("update-indicator-dark", dark = true)

  private fun render(name: String, dark: Boolean) {
    // The drop loop waits on `delay`, which only a test dispatcher lets us fast-forward
    val dispatcher = StandardTestDispatcher()
    val scene = ImageComposeScene(
      width = 180 * SCALE,
      height = 64 * SCALE,
      density = Density(SCALE.toFloat()),
      coroutineContext = dispatcher,
    ) {
      CampfireTheme(useDarkColors = dark) {
        Surface(color = MaterialTheme.colorScheme.surface) {
          Row(Modifier.padding(12.dp)) {
            ThemeIcon(ConnectionState.Connected)
            ThemeIcon(ConnectionState.Connecting, Modifier.padding(start = 16.dp))
            ThemeIcon(ConnectionState.Disconnected, Modifier.padding(start = 16.dp))
          }
        }
      }
    }
    scene.use {
      // Sample across the first drop so the arrow is caught mid-flight as well as at rest
      var elapsed = 0L
      // Each animation phase starts on the frame after the last one ends, so sample densely
      (listOf(0L) + (0..14).map { 2_900L + it * 60L }).forEachIndexed { index, millis ->
        dispatcher.scheduler.advanceTimeBy(millis - elapsed)
        dispatcher.scheduler.runCurrent()
        elapsed = millis
        val image = it.render(nanoTime = millis * 1_000_000L)
        dispatcher.scheduler.runCurrent()
        val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
        val file = File(renders, "$name-$index.png")
        file.writeBytes(bytes)
        println("rendered ${file.absolutePath}")
        assertTrue(bytes.size > 500)
      }
    }
  }

  @androidx.compose.runtime.Composable
  private fun ThemeIcon(connection: ConnectionState, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp)) {
      Box(
        Modifier
          .padding(4.dp)
          .size(32.dp)
          .background(Color(0xFF7A9E5B), RoundedCornerShape(8.dp)),
      ) {
        ConnectionIndicator(
          state = connection,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 2.dp, end = 2.dp),
        )
      }
      UpdateIndicator(Modifier.align(Alignment.BottomEnd))
    }
  }

  private companion object {
    const val SCALE = 4
  }
}
