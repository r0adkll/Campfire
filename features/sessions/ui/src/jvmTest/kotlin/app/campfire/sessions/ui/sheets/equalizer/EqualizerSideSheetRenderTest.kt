// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.equalizer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.core.audio.EqualizerProfile
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The equalizer is the sheet that most wants room: ten vertical faders, a preset row, and two
 * sliders. This renders it in the side sheet at the sizes that select one, so the sheet's
 * content-driven width can be eyeballed against the one body that really exercises it.
 */
@OptIn(ExperimentalComposeUiApi::class)
class EqualizerSideSheetRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `the equalizer in a side sheet on a landscape phone`() {
    render("equalizer-side-sheet", width = 914, height = 411)
  }

  @Test
  fun `the equalizer in a side sheet on a foldable's lower half`() {
    render("equalizer-side-sheet-fold", width = 840, height = 340)
  }

  private fun render(name: String, width: Int, height: Int) {
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())

    ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Host(sizeClass) },
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

  @Composable
  private fun Host(sizeClass: WindowSizeClass) {
    CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
      CampfireTheme(useDarkColors = false) {
        val overlayHost = rememberOverlayHost()
        ContentWithOverlays(overlayHost = overlayHost, modifier = Modifier.fillMaxSize()) {
          Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {}
        }

        LaunchedEffect(Unit) {
          overlayHost.show(
            AdaptiveSheetOverlay<Unit, Unit>(
              model = Unit,
              onDismiss = { },
              skipPartiallyExpanded = true,
            ) { _, _ ->
              // The pure body, driven by a plain profile: this is about the sheet around it.
              EqualizerSheet(
                profile = EqualizerProfile(enabled = true),
                available = true,
                perBookEnabled = false,
                onEnabledChange = {},
                onPresetSelected = {},
                onBandGainChange = { _, _ -> },
                onLoudnessChange = {},
                onBassBoostChange = {},
                onPerBookChange = {},
              )
            },
          )
        }
      }
    }
  }
}
