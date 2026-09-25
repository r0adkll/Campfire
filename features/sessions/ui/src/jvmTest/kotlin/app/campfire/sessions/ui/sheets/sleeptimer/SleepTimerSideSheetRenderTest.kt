// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.sleeptimer

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
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.test.FakeSleepSettings
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The sleep timer is the tallest of the sheets: a mode selector, a time input, a preset slider, a
 * two-line option row and a full-width action, none of which shrink. This renders it at the sizes
 * that pick a side sheet, which are exactly the sizes with the least height to spend.
 */
@OptIn(ExperimentalComposeUiApi::class)
class SleepTimerSideSheetRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `the sleep timer in a side sheet on a landscape phone`() {
    render("sleep-timer-side-sheet", width = 914, height = 411)
  }

  @Test
  fun `the sleep timer in a side sheet on the shortest landscape phone`() {
    render("sleep-timer-side-sheet-short", width = 780, height = 360)
  }

  @Test
  fun `the sleep timer as a bottom sheet, for comparison`() {
    render("sleep-timer-bottom-sheet", width = 411, height = 914)
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
              // No running timer: the "set one" body, which is the tall one.
              TimerBottomSheetV2(
                runningTimer = null,
                onTimerSelected = {},
                onTimerCleared = {},
                component = FakeComponent,
              )
            },
          )
        }
      }
    }
  }

  private object FakeComponent : SleepTimerBottomSheetComponent {
    override val sessionsRepository: SessionsRepository = FakeSessionsRepository()
    override val sleepSettings: SleepSettings = FakeSleepSettings()
  }
}
