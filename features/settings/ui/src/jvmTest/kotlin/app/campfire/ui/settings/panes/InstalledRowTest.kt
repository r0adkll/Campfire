// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.BuildCommit
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle
import app.campfire.ui.settings.AppUpdatesSettingsInfo
import app.campfire.updates.AppUpdateCheck
import app.campfire.updates.source.AppUpdateSheet
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

@OptIn(ExperimentalTestApi::class)
class InstalledRowTest {

  private val clipboard = FakeClipboard()
  private val toasts = mutableListOf<String>()

  @Test
  fun `alpha builds show the commit time and subject under the version`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = true)

    onNodeWithText("1.4.0 (1040099) [220c0be]").assertIsDisplayed()
    onNodeWithText("2026-11-24 09:03 -05:00").assertIsDisplayed()
    onNodeWithText("Show the build commit in App updates").assertIsDisplayed()
  }

  @Test
  fun `alpha builds copy every line on long-press`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = true)

    onNodeWithText("Installed").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
    onNodeWithText("Installed").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo(
      "1.4.0 (1040099) [220c0be]\n2026-11-24 09:03 -05:00\nShow the build commit in App updates",
    )
    assertThat(toasts).containsExactly("Copied to clipboard")
  }

  @Test
  fun `alpha builds skip whatever the commit doesn't have`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = true, commit = BuildCommit(time = null, sha = "220c0be", subject = null))

    onNodeWithText("Installed").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isEqualTo("1.4.0 (1040099) [220c0be]")
  }

  @Test
  fun `other builds show only the version and sha`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = false)

    onNodeWithText("1.4.0 (1040099) [220c0be]").assertIsDisplayed()
    onNodeWithText("2026-11-24", substring = true).assertDoesNotExist()
    onNodeWithText("Show the build commit", substring = true).assertDoesNotExist()
  }

  @Test
  fun `other builds offer no long-press`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = false)

    onNodeWithText("Installed").assertIsNotEnabled()
    onNodeWithText("Installed").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
    onNodeWithText("Installed").performTouchInput { longClick() }
    waitForIdle()

    assertThat(clipboard.text).isNull()
  }

  @Test
  fun `no git info is just the version`() = runComposeUiTest {
    setInstalledRow(showCommitDetails = true, commit = null)

    onNodeWithText("1.4.0 (1040099)").assertIsDisplayed()
  }

  private fun ComposeUiTest.setInstalledRow(
    showCommitDetails: Boolean,
    commit: BuildCommit? = BuildCommit(
      time = Instant.parse("2026-11-24T14:03:27Z"),
      sha = "220c0be",
      subject = "Show the build commit in App updates",
    ),
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
          InstalledRow(
            info = AppUpdatesSettingsInfo(
              update = null,
              check = AppUpdateCheck(),
              installedVersion = "1.4.0",
              installedBuild = 1040099L,
              installedCommit = commit,
              showCommitDetails = showCommitDetails,
              channel = null,
              sheet = NoSheet,
              showSheet = false,
            ),
            timeZone = UtcOffset(hours = -5).asTimeZone(),
          )
        }
      }
    }
  }

  private object NoSheet : AppUpdateSheet {
    @Composable
    override fun Content(onDismiss: () -> Unit) = Unit
  }
}
