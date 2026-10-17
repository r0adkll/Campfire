// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.network.reachability.OfflineReason
import app.campfire.network.reachability.OfflineState
import app.campfire.network.reachability.Reachability
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the work-offline section of Settings → Connection off-screen in each state, plus its
 * choice dialog, and writes PNGs to `build/renders` for eyeballing; the assertions only guard that
 * each state paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class WorkOfflineSettingsRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val online = OfflineState(reason = null, onWifi = true)

  @Test
  fun `online and connected`() {
    render("work-offline-online") { Section(online, Reachability.Reachable) }
  }

  @Test
  fun `online but unreachable`() {
    render("work-offline-unreachable", dark = true) { Section(online, Reachability.Unreachable) }
  }

  @Test
  fun `offline for a while`() {
    val inAnHour = OfflineReason.Until(Clock.System.now().plus(1.hours).toEpochMilliseconds())
    render("work-offline-timed") { Section(OfflineState(inAnHour, onWifi = true), Reachability.Offline) }
  }

  @Test
  fun `offline on mobile data`() {
    render("work-offline-mobile-data", dark = true) {
      Section(OfflineState(OfflineReason.OnMobileData, onWifi = false), Reachability.Offline)
    }
  }

  @Test
  fun `choosing how long`() {
    // The card's "Work offline…" button, at its bottom end
    render("work-offline-dialog", tap = Offset(x = 296 * 2f, y = 108 * 2f)) {
      Section(online, Reachability.Reachable)
    }
  }

  @Composable
  private fun Section(offline: OfflineState, reachability: Reachability) {
    WorkOfflineSettings(
      offline = offline,
      canDetectWifi = true,
      workOfflineOnMobileData = offline.reason == OfflineReason.OnMobileData,
      serverName = "Testbed",
      reachability = reachability,
      onEvent = {},
    )
  }

  private fun render(
    name: String,
    dark: Boolean = false,
    tap: Offset? = null,
    content: @Composable () -> Unit,
  ) {
    val scene = ImageComposeScene(width = WIDTH * 2, height = HEIGHT * 2, density = Density(2f)) {
      CampfireTheme(useDarkColors = dark) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
          content()
        }
      }
    }
    scene.use {
      it.render()
      if (tap != null) {
        it.sendPointerEvent(PointerEventType.Press, tap)
        it.sendPointerEvent(PointerEventType.Release, tap)
        for (frame in 1..30) it.render(nanoTime = frame * 50_000_000L)
      }
      val image = it.render(nanoTime = 2_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    const val WIDTH = 400
    const val HEIGHT = 480
  }
}
