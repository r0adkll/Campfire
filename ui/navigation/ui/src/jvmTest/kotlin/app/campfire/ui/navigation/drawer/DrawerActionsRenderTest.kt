// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.network.reachability.OfflineReason
import app.campfire.network.reachability.OfflineState
import app.campfire.settings.api.ThemeMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the drawer footer off-screen — online, and working offline for each reason, in light and
 * dark — plus its work-offline menu open, and writes PNGs to `build/renders` for eyeballing; the
 * assertions only guard that each state paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DrawerActionsRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val online = OfflineState(reason = null, onWifi = true)
  private val inAnHour = OfflineState(
    reason = OfflineReason.Until(Clock.System.now().plus(1.hours).toEpochMilliseconds()),
    onWifi = true,
  )
  private val onMobileData = OfflineState(reason = OfflineReason.OnMobileData, onWifi = false)

  @Test
  fun `drawer actions light`() {
    render("drawer-actions-light", height = FOOTERS_HEIGHT) { Footers(dark = false) }
  }

  @Test
  fun `drawer actions dark`() {
    render("drawer-actions-dark", height = FOOTERS_HEIGHT) { Footers(dark = true) }
  }

  @Test
  fun `menu while online`() {
    render("drawer-menu-online", height = MENU_HEIGHT, tapWorkOffline = true) { Footer(dark = false, online) }
  }

  @Test
  fun `menu while offline for a while`() {
    render("drawer-menu-offline-timed", height = MENU_HEIGHT, tapWorkOffline = true) {
      Footer(dark = true, inAnHour)
    }
  }

  @Test
  fun `menu while offline on mobile data`() {
    render("drawer-menu-offline-mobile-data", height = MENU_HEIGHT, tapWorkOffline = true) {
      Footer(dark = false, onMobileData)
    }
  }

  @Composable
  private fun Footers(dark: Boolean) {
    DrawerSurface(dark) {
      Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(online, inAnHour).forEach { Actions(it) }
      }
    }
  }

  /** One footer pinned to the bottom, like the drawer's, so its menu has room to open upwards. */
  @Composable
  private fun Footer(dark: Boolean, offline: OfflineState) {
    DrawerSurface(dark) {
      Box(Modifier.fillMaxSize().padding(vertical = 8.dp), contentAlignment = Alignment.BottomStart) {
        Actions(offline)
      }
    }
  }

  @Composable
  private fun Actions(offline: OfflineState) {
    DrawerActions(
      themeMode = ThemeMode.SYSTEM,
      offline = offline,
      canDetectWifi = true,
      onGoOffline = {},
      onGoOnline = {},
      onCycleThemeMode = {},
      onChangeTheme = {},
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    )
  }

  /** The drawer sheet's surface, which sets the content color the outlined buttons draw in. */
  @Composable
  private fun DrawerSurface(dark: Boolean, content: @Composable () -> Unit) {
    CampfireTheme(useDarkColors = dark) {
      Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, content = content)
    }
  }

  private fun render(
    name: String,
    height: Int,
    tapWorkOffline: Boolean = false,
    content: @Composable () -> Unit,
  ) {
    ImageComposeScene(width = WIDTH * 2, height = height * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      if (tapWorkOffline) {
        // The work-offline button: 16dp in from the start, centered in the bottom footer
        val button = Offset(x = (16 + 26) * 2f, y = (height - 8 - 24) * 2f)
        scene.sendPointerEvent(PointerEventType.Press, button)
        scene.sendPointerEvent(PointerEventType.Release, button)
        // Step through the menu's enter animation
        for (frame in 1..30) scene.render(nanoTime = frame * 50_000_000L)
      }
      val image = scene.render(nanoTime = 2_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    const val WIDTH = 320
    const val FOOTERS_HEIGHT = 136
    const val MENU_HEIGHT = 420
  }
}
