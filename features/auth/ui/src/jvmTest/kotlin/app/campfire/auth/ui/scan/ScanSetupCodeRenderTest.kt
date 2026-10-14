// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.qrscanner.PreviewQrCodeScanner
import app.campfire.qrscanner.PreviewQrCodeScanner.PreviewCamera
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the scan screen over a stand-in for the camera's view and writes PNGs to
 * `build/renders` for eyeballing. The assertion only guards that each composes and paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScanSetupCodeRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `waiting for a code`() {
    render("scan-waiting")
  }

  @Test
  fun `another qr code`() {
    render("scan-other-code", sawOtherCode = true)
  }

  @Test
  fun `a setup code was read`() {
    render("scan-captured", captured = true)
  }

  @Test
  fun `camera permission denied`() {
    render("scan-permission-denied", camera = PreviewCamera.PermissionDenied)
  }

  private fun render(
    name: String,
    camera: PreviewCamera = PreviewCamera.Live,
    sawOtherCode: Boolean = false,
    captured: Boolean = false,
  ) {
    for (dark in listOf(false, true)) {
      render("$name-${if (dark) "dark" else "light"}") {
        CampfireTheme(useDarkColors = dark) {
          ScanSetupCodeUi(
            state = ScanSetupCodeUiState(sawOtherCode = sawOtherCode, captured = captured, eventSink = {}),
            qrCodeScanner = PreviewQrCodeScanner(camera),
            modifier = Modifier,
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
