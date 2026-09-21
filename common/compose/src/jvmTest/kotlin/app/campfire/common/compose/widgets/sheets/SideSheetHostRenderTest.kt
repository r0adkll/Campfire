// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The two hosts that do **not** give the sheet the whole window, rendered the way the app builds
 * them, so the sheet can be eyeballed where it actually goes wrong:
 *
 * - the landscape phone, where the expanded player is capped at 700dp and pinned to the bottom
 *   corner, so its overlay region is a floating box rather than the screen;
 * - a half-open foldable, where the player is docked into the region below the hinge.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SideSheetHostRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `the sheet inside the landscape phone's floating player`() {
    render("host-floating-player", width = 914, height = 411) {
      // LoggedIn caps the bar at 700dp and aligns it to the bottom-start corner.
      Box(Modifier.fillMaxSize()) {
        Box(
          Modifier
            .align(Alignment.BottomStart)
            .widthIn(max = 700.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        ) {
          PlayerWithSheet(regionWidth = 700, regionHeight = 411)
        }
      }
    }
  }

  @Test
  fun `the sheet inside a docked player below the hinge`() {
    render("host-docked-player", width = 840, height = 700) {
      Column(Modifier.fillMaxSize()) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(Color(0xFF2E7D32)),
        ) {
          Text("above the hinge", Modifier.padding(16.dp), color = Color.White)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
          PlayerWithSheet(regionWidth = 840, regionHeight = 320)
        }
      }
    }
  }

  /** Mirrors how the player hosts its own overlays: its own size class, its own host, filling. */
  @Composable
  private fun PlayerWithSheet(regionWidth: Int, regionHeight: Int) {
    val regionSizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(regionWidth.toFloat(), regionHeight.toFloat())

    CompositionLocalProvider(LocalWindowSizeClass provides regionSizeClass) {
      val overlayHost = rememberOverlayHost()
      ContentWithOverlays(overlayHost = overlayHost, modifier = Modifier.fillMaxSize()) {
        // The player draws itself as a rounded surface inset from its region, the way the
        // expanded bar does.
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(32.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          Text("the player", Modifier.padding(24.dp))
        }
      }

      LaunchedEffect(Unit) {
        overlayHost.show(
          AdaptiveSheetOverlay<Unit, Unit>(model = Unit, onDismiss = { }) { _, _ ->
            Column(Modifier.fillMaxWidth()) {
              Text("Chapters", Modifier.padding(16.dp))
              repeat(5) {
                Box(
                  Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                )
              }
            }
          },
        )
      }
    }
  }

  private fun render(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
    ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = {
        CampfireTheme(useDarkColors = false) {
          Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            content()
          }
        }
      },
    ).use { scene ->
      repeat(10) { frame -> scene.render(frame * 100_000_000L) }
      val bytes = scene.render(nanoTime = 3_000_000_000L)
        .encodeToData(EncodedImageFormat.PNG)!!
        .bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertThat(bytes.size).isGreaterThan(1_000)
    }
  }
}
