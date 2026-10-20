// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.ui.settings.AppUpdatesSettingsInfo
import app.campfire.updates.AppUpdateCheck
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateSheet
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders Settings → App updates off-screen in each state and writes PNGs to `build/renders` for
 * eyeballing; the assertions only guard that each state paints.
 */
class UpdatesSettingsRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `update available`() {
    render("updates-available") { Pane(info(UPDATE)) }
  }

  @Test
  fun `update available dark`() {
    render("updates-available-dark", dark = true) { Pane(info(UPDATE)) }
  }

  @Test
  fun `downloading`() {
    render("updates-downloading") { Pane(info(UPDATE.copy(status = AppUpdate.Status.Downloading(0.42f)))) }
  }

  @Test
  fun `needs a reinstall`() {
    render("updates-reinstall") { Pane(info(UPDATE.copy(status = AppUpdate.Status.ReinstallRequired))) }
  }

  @Test
  fun `up to date`() {
    render("updates-up-to-date") { Pane(info(update = null)) }
  }

  @Test
  fun `up to date dark`() {
    render("updates-up-to-date-dark", dark = true) { Pane(info(update = null)) }
  }

  @Test
  fun `pull request build`() {
    render("updates-pull-request") { Pane(info(UPDATE, channel = "pr/1290")) }
  }

  @Test
  fun `alpha build up to date`() {
    render("updates-up-to-date-alpha") { Pane(info(update = null, channel = "main")) }
  }

  @Test
  fun `checking`() {
    render("updates-checking") { Pane(info(update = null, check = AppUpdateCheck(isChecking = true))) }
  }

  @Test
  fun `couldn't check`() {
    render("updates-check-failed") { Pane(info(update = null, check = CHECKED.copy(failed = true))) }
  }

  @Composable
  private fun Pane(info: AppUpdatesSettingsInfo) {
    UpdatesSettings(
      info = info,
      onEvent = {},
      modifier = Modifier.padding(vertical = 16.dp),
    )
  }

  private fun info(
    update: AppUpdate?,
    check: AppUpdateCheck = CHECKED,
    channel: String = "beta",
  ) = AppUpdatesSettingsInfo(
    update = update,
    check = check,
    installedVersion = "1.3.2",
    installedBuild = 1_003_020L,
    channel = channel,
    sheet = NoSheet,
    showSheet = false,
  )

  private fun render(
    name: String,
    dark: Boolean = false,
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
      val image = it.render(nanoTime = 2_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private object NoSheet : AppUpdateSheet {
    @Composable
    override fun Content(onDismiss: () -> Unit) = Unit
  }

  private companion object {
    const val WIDTH = 400
    const val HEIGHT = 820

    val CHECKED = AppUpdateCheck(checkedAt = Clock.System.now() - 5.minutes)

    val UPDATE = AppUpdate(
      versionName = "1.4.0",
      versionCode = 1_004_000L,
      headline = "Dedicated App updates settings and a fresh update card",
      releaseNotes = """
        ## What's Changed
        - App updates get their own page in Settings
        - The drawer's update card shows progress while it downloads
        - Settings is badged while an update waits
      """.trimIndent(),
      sizeBytes = 48_200_000L,
      publishedAt = Clock.System.now() - 2.hours,
      channel = "beta",
      pullRequest = 1290,
    )
  }
}
