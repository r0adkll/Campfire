// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
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
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class VersionSettingTest {

  private val clipboard = FakeClipboard()
  private val toasts = mutableListOf<String>()
  private var clicks = 0

  @Test
  fun `shows the version with the short sha and nothing else`() = runComposeUiTest {
    setVersionSetting(onClick = null)

    onNodeWithText("1.4.0 (1040099) [220c0be]").assertIsDisplayed()
    onNodeWithText("2026-11-24", substring = true).assertDoesNotExist()
    onNodeWithText("Ship it", substring = true).assertDoesNotExist()
  }

  @Test
  fun `drops the sha without git info`() = runComposeUiTest {
    setVersionSetting(onClick = null, buildCommit = null)

    onNodeWithText("1.4.0 (1040099)").assertIsDisplayed()
    onNodeWithText("220c0be", substring = true).assertDoesNotExist()
  }

  @Test
  fun `long-press copies the version and confirms it`() = runComposeUiTest {
    setVersionSetting(onClick = { clicks++ })

    onNodeWithText("Version").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) [220c0be]")
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
    onNodeWithText("Version").assert(hasLongClickLabel("Copy build info"))

    onNodeWithText("Version").performTouchInput { click() }
    waitForIdle()
    assertThat(clipboard.text).isNull()

    onNodeWithText("Version").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) [220c0be]")
  }

  @Test
  fun `once developer mode is on the keyboard can still copy`() = runComposeUiTest {
    setVersionSetting(onClick = null)

    onNodeWithText("Version").requestFocus()
    onNodeWithText("Version").performKeyInput { pressKey(Key.Enter) }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) [220c0be]")
    assertThat(toasts).containsExactly("Copied to clipboard")
  }

  private fun ComposeUiTest.setVersionSetting(
    onClick: (() -> Unit)?,
    buildCommit: BuildCommit? = BuildCommit(Instant.parse("2026-11-24T14:03:27Z"), "220c0be", "Ship it"),
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
}
