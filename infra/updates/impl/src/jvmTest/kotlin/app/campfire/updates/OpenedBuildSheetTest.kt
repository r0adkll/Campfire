// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateSheet
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalTestApi::class)
class OpenedBuildSheetTest {

  private val appUpdates = FakeAppUpdates()
  private val toasts = mutableListOf<String>()
  private val toast = Toast { message, _ ->
    toasts += message
    ToastHandle {}
  }

  @Test
  fun `an offered build shows the install sheet until it's dismissed`() = runComposeUiTest {
    appUpdates.openedBuild.value = OpenedBuild.Offered
    setContent { OpenedBuildSheet(appUpdates) }

    onNodeWithText(SHEET).assertIsDisplayed().performClick()

    assertThat(appUpdates.closed).isEqualTo(1)
    onNodeWithText(SHEET).assertDoesNotExist()
  }

  @Test
  fun `a build that couldn't be opened toasts and closes`() = runComposeUiTest {
    appUpdates.openedBuild.value = OpenedBuild.Failed
    setContent {
      CompositionLocalProvider(LocalToast provides toast) { OpenedBuildSheet(appUpdates) }
    }
    waitForIdle()

    assertThat(toasts).containsExactly("Couldn't open that build. Check your connection and try again.")
    assertThat(appUpdates.closed).isEqualTo(1)
  }

  @Test
  fun `nothing opened shows nothing`() = runComposeUiTest {
    setContent {
      CompositionLocalProvider(LocalToast provides toast) { OpenedBuildSheet(appUpdates) }
    }

    onNodeWithText(SHEET).assertDoesNotExist()
    assertThat(toasts).isEmpty()
  }

  private class FakeAppUpdates : AppUpdates {
    override val isSupported = true
    override val channel: String? = null
    override val update = MutableStateFlow<AppUpdate?>(null)
    override val undismissedUpdate = MutableStateFlow<AppUpdate?>(null)
    override val check = MutableStateFlow(AppUpdateCheck())
    override val openedBuild = MutableStateFlow<OpenedBuild?>(null)
    var closed = 0

    override val installSheet = object : AppUpdateSheet {
      @Composable
      override fun Content(onDismiss: () -> Unit) {
        Text(SHEET, Modifier.clickable(onClick = onDismiss))
      }
    }

    override fun refresh() = Unit
    override fun dismiss(update: AppUpdate) = Unit
    override fun openBuild(buildId: String) = Unit

    override fun closeOpenedBuild() {
      closed++
      openedBuild.value = null
    }
  }

  private companion object {
    const val SHEET = "Install this build"
  }
}
