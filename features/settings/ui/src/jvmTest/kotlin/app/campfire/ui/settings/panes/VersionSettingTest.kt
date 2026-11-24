// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.BuildCommit
import app.campfire.core.app.Flavor
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

@OptIn(ExperimentalTestApi::class)
class VersionSettingTest {

  private val clipboard = FakeClipboard()
  private val toasts = mutableListOf<String>()
  private var clicks = 0

  @Test
  fun `shows the version and the commit it was built from`() = runComposeUiTest {
    setVersionSetting(onClick = null)

    onNodeWithText("1.4.0 (1040099)").assertIsDisplayed()
    onNodeWithText("2026-11-24 09:03 -05:00 · 220c0be").assertIsDisplayed()
  }

  @Test
  fun `hides the commit line without git info`() = runComposeUiTest {
    setVersionSetting(onClick = null, buildCommit = null)

    onNodeWithText("1.4.0 (1040099)").assertIsDisplayed()
    onNodeWithText("220c0be", substring = true).assertDoesNotExist()
  }

  @Test
  fun `long-press copies the version and commit and confirms it`() = runComposeUiTest {
    setVersionSetting(onClick = { clicks++ })

    onNodeWithText("Version").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) · 2026-11-24 09:03 -05:00 · 220c0be")
    assertThat(toasts).containsExactly("Copied to clipboard")
    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `a tap still counts toward developer mode`() = runComposeUiTest {
    setVersionSetting(onClick = { clicks++ })

    onNodeWithText("Version").assertHasClickAction()
    onNodeWithText("Version").performClick()

    assertThat(clicks).isEqualTo(1)
  }

  @Test
  fun `once developer mode is on the row offers only the long-press`() = runComposeUiTest {
    setVersionSetting(onClick = null)

    onNodeWithText("Version").assertHasNoClickAction()
    onNodeWithText("Version").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

    onNodeWithText("Version").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) · 2026-11-24 09:03 -05:00 · 220c0be")
  }

  @Test
  fun `once developer mode is on the keyboard can still copy`() = runComposeUiTest {
    setVersionSetting(onClick = null)

    onNodeWithText("Version").requestFocus()
    onNodeWithText("Version").performKeyInput { pressKey(Key.Enter) }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) · 2026-11-24 09:03 -05:00 · 220c0be")
    assertThat(toasts).containsExactly("Copied to clipboard")
  }

  private fun ComposeUiTest.setVersionSetting(
    onClick: (() -> Unit)?,
    buildCommit: BuildCommit? = BuildCommit(Instant.parse("2026-11-24T14:03:27Z"), "220c0be"),
  ) {
    setContent {
      CampfireTheme {
        CompositionLocalProvider(
          LocalClipboard provides clipboard,
          LocalToast provides Toast { message, _ ->
            toasts += message
            ToastHandle {}
          },
        ) {
          VersionSetting(
            applicationInfo = applicationInfo(buildCommit),
            onClick = onClick,
            timeZone = UtcOffset(hours = -5).asTimeZone(),
          )
        }
      }
    }
  }

  private fun applicationInfo(buildCommit: BuildCommit?) = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = false,
    flavor = Flavor.Standard,
    versionName = "1.4.0",
    versionCode = 1040099,
    osName = "Test",
    osVersion = "1",
    buildCommit = buildCommit,
  )

  private class FakeClipboard : Clipboard {
    private var entry: ClipEntry? = null

    @OptIn(ExperimentalComposeUiApi::class)
    val text: String?
      get() = (entry?.nativeClipEntry as? Transferable)?.getTransferData(DataFlavor.stringFlavor) as? String

    override suspend fun getClipEntry(): ClipEntry? = entry

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
      entry = clipEntry
    }

    override val nativeClipboard: NativeClipboard
      get() = error("Not used")
  }
}
