// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * The claim the whole adaptive-sheet design rests on: a sheet must stay inside the region that
 * hosted it.
 *
 * It matters because of the half-open foldable. The docked player gets its own `ContentWithOverlays`
 * covering only the region below the hinge, so a sheet it opens should cover only that region. A
 * `ModalBottomSheet` does not — it renders into a `ModalBottomSheetDialog`, which is a layer over
 * the whole window and knows nothing about the box that launched it.
 *
 * The scene here is deliberately taller than the host region: the lower 320dp hosts the overlays,
 * the upper 380dp is plain content standing in for the screen above the hinge. Whether the upper
 * half changes when a sheet opens is the whole question.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SheetRegionContainmentTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val sceneWidth = 840
  private val sceneHeight = 700
  private val regionHeight = 320

  @Test
  fun `the side panel stays inside the region that hosted it`() {
    val closed = render("region-side-closed", sheet = false)
    val open = render("region-side-open", sheet = true)

    // The region itself changed -- the panel is in there.
    assertThat(differingRowsIn(open, closed, from = sceneHeight - regionHeight, to = sceneHeight))
      .isGreaterThan(0)

    // Nothing above it did.
    assertThat(differingRowsIn(open, closed, from = 0, to = sceneHeight - regionHeight))
      .isEqualTo(0)
  }

  /**
   * The counterfactual, so the test above is decisive rather than merely consistent: the same
   * region, the same host, a plain [BottomSheetOverlay] instead. Its dialog layer knows nothing of
   * the box that launched it, so it scrims the region above the hinge too.
   */
  @Test
  fun `a plain bottom sheet escapes the region and covers the content above it`() {
    val closed = render("region-bottom-closed", sheet = false)
    val open = render("region-bottom-open", sheet = true, bottomSheet = true)

    assertThat(differingRowsIn(open, closed, from = 0, to = sceneHeight - regionHeight))
      .isGreaterThan(0)
  }

  private fun render(name: String, sheet: Boolean, bottomSheet: Boolean = false): Image {
    // A wide, short region picks the side panel, which is the case a docked player is in.
    val regionSizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(sceneWidth.toFloat(), regionHeight.toFloat())

    return ImageComposeScene(
      width = sceneWidth,
      height = sceneHeight,
      density = Density(1f),
      content = { Split(regionSizeClass, sheet, bottomSheet) },
    ).use { scene ->
      repeat(8) { frame -> scene.render(frame * 100_000_000L) }
      val image = scene.render(nanoTime = 2_000_000_000L)
      File(renders, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
      image
    }
  }

  /** How many pixel rows in `[from, to)` differ between the two renders. */
  private fun differingRowsIn(a: Image, b: Image, from: Int, to: Int): Int {
    val pixelsA = a.peekPixels()!!.buffer.bytes
    val pixelsB = b.peekPixels()!!.buffer.bytes
    val stride = pixelsA.size / a.height

    return (from until to).count { row ->
      val start = row * stride
      (start until start + stride).any { i -> pixelsA[i] != pixelsB[i] }
    }
  }

  @Composable
  private fun Split(regionSizeClass: WindowSizeClass, sheet: Boolean, bottomSheet: Boolean) {
    CampfireTheme(useDarkColors = false) {
      Column(Modifier.fillMaxSize()) {
        // Above the hinge: ordinary content, with no overlay host of its own.
        Box(
          Modifier
            .fillMaxWidth()
            .height((sceneHeight - regionHeight).dp)
            .background(Color(0xFF2E7D32)),
        ) {
          Text("above the hinge", Modifier.padding(16.dp), color = Color.White)
        }

        // Below it: the docked player's region, with its own host and its own size class.
        Box(Modifier.fillMaxWidth().height(regionHeight.dp)) {
          CompositionLocalProvider(LocalWindowSizeClass provides regionSizeClass) {
            val overlayHost = rememberOverlayHost()
            ContentWithOverlays(overlayHost = overlayHost, modifier = Modifier.fillMaxSize()) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(),
              ) {
                Text("the docked player", Modifier.padding(16.dp))
              }
            }

            if (sheet) {
              LaunchedEffect(Unit) {
                val body: @Composable (Unit, Any) -> Unit = { _, _ ->
                  Column(Modifier.fillMaxWidth()) {
                    Text("Chapters", Modifier.padding(16.dp))
                  }
                }
                if (bottomSheet) {
                  overlayHost.show(
                    BottomSheetOverlay<Unit, Unit>(
                      model = Unit,
                      onDismiss = { },
                    ) { m, n -> body(m, n) },
                  )
                } else {
                  overlayHost.show(
                    AdaptiveSheetOverlay<Unit, Unit>(model = Unit, onDismiss = { }) { m, n ->
                      body(m, n)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
