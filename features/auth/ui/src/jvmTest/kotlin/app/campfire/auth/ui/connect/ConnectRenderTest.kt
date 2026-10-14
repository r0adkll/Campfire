// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the setup link screens and writes PNGs to `build/renders` for eyeballing. The
 * assertion only guards that each composes and paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ConnectRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val newServer = ConnectContent.Confirm(
    serverHost = "abs.example.com",
    serverName = "Example Audiobooks",
    existingUserName = null,
    signInWithOpenId = true,
  )

  @Test
  fun `confirming a new server`() {
    render("connect-confirm-new", newServer)
  }

  @Test
  fun `saving a new server`() {
    render("connect-confirm-saving", newServer.copy(isSaving = true))
  }

  @Test
  fun `confirming a token update`() {
    render("connect-confirm-update", newServer.copy(existingUserName = "alice"))
  }

  @Test
  fun `a broken link`() {
    render("connect-invalid", ConnectContent.Invalid)
  }

  @Test
  fun `an updated token`() {
    render("connect-updated", ConnectContent.Updated(serverHost = "abs.example.com", userName = "alice"))
  }

  private fun render(name: String, content: ConnectContent) {
    for (dark in listOf(false, true)) {
      render("$name-${if (dark) "dark" else "light"}") {
        CampfireTheme(useDarkColors = dark) {
          Connect(state = ConnectUiState(content = content, eventSink = {}), modifier = Modifier)
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
