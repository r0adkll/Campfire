// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import org.jetbrains.skia.EncodedImageFormat

/**
 * The date picker dialog with its line under the calendar. A PNG lands in `build/renders` for
 * eyeballing.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class CampfireDatePickerDialogTest {

  @Test
  fun theSupportingLineSitsUnderThePicker() = runComposeUiTest {
    setContent { Dialog(onConfirm = {}) }

    val title = onNodeWithText("Finished on").getBoundsInRoot()
    val supporting = onNodeWithTag("date_picker_supporting_text").getBoundsInRoot()
    assertThat(supporting.top).isGreaterThan(title.bottom)
  }

  @Test
  fun savingGivesThePickedDay() = runComposeUiTest {
    val picked = mutableListOf<LocalDate>()
    setContent { Dialog(onConfirm = { picked += it }) }

    onNodeWithTag("date_picker_confirm").performClick()

    assertThat(picked).containsExactly(LocalDate(2026, 4, 20))
  }

  @Test
  fun render() {
    val renders = File("build/renders").apply { mkdirs() }
    val bytes = ImageComposeScene(
      width = 412 * 2,
      height = 915 * 2,
      density = Density(2f),
      content = { Dialog(onConfirm = {}) },
    ).use { scene ->
      repeat(5) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
    File(renders, "date-picker-dialog.png").writeBytes(bytes)
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  @Composable
  private fun Dialog(onConfirm: (LocalDate) -> Unit) {
    CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 412, minHeightDp = 915)) {
      CampfireTheme(useDarkColors = false) {
        CampfireDatePickerDialog(
          title = "Finished on",
          initialDate = LocalDate(2026, 4, 20),
          earliest = LocalDate(2026, 4, 3),
          latest = LocalDate(2026, 4, 24),
          supportingText = "Started April 3, 2026, so earlier days can't be picked",
          onConfirm = onConfirm,
          onDismissRequest = {},
        )
      }
    }
  }
}
