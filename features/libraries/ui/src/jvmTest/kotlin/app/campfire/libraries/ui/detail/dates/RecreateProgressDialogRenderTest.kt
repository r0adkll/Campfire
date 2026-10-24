// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import org.jetbrains.skia.EncodedImageFormat

/**
 * The confirmation before recreating a progress, with the longest dates it shows. A PNG lands in
 * `build/renders` for eyeballing.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class RecreateProgressDialogRenderTest {

  @Test
  fun aLongNewStartStaysOnOneLine() = runComposeUiTest {
    setContent { Dialog() }

    val layouts = mutableListOf<TextLayoutResult>()
    onNodeWithTag("recreate_progress_new_start")
      .fetchSemanticsNode()
      .config[SemanticsActions.GetTextLayoutResult]
      .action
      ?.invoke(layouts)
    assertThat(layouts.single().lineCount).isEqualTo(1)
  }

  @Test
  fun render() {
    val renders = File("build/renders").apply { mkdirs() }
    val bytes = ImageComposeScene(
      width = 412 * 2,
      height = 915 * 2,
      density = Density(2f),
      content = { Dialog() },
    ).use { scene ->
      repeat(5) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
    File(renders, "recreate-progress-dialog.png").writeBytes(bytes)
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  @Composable
  private fun Dialog() {
    CampfireTheme(useDarkColors = false) {
      RecreateProgressDialog(
        from = LocalDate(2026, 9, 30),
        to = LocalDate(2026, 12, 25),
        onConfirm = {},
        onDismissRequest = {},
      )
    }
  }
}
