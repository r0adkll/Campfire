// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.BuildCommit
import app.campfire.core.app.Flavor
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle
import app.campfire.ui.settings.AboutSettingsInfo
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class AboutPaneTest {

  private val events = mutableListOf<SettingsUiEvent>()
  private val toasts = mutableListOf<String>()

  @Test
  fun `each link sends its event`() = runComposeUiTest {
    setAboutPane()

    listOf("Made by", "Contribute", "Privacy policy", "Terms of service", "Open source attributions", "Changelog")
      .forEach { onNodeWithText(it).performScrollTo().performClick() }

    assertThat(events).containsExactly(
      AboutSettingEvent.DeveloperClick,
      AboutSettingEvent.GithubClick,
      AboutSettingEvent.PrivacyPolicyClick,
      AboutSettingEvent.TermsOfServiceClick,
      AboutSettingEvent.AttributionsClick,
      AboutSettingEvent.ChangelogClick,
    )
  }

  @Test
  fun `the data collection switches toggle their setting`() = runComposeUiTest {
    setAboutPane(AboutSettingsInfo(crashReportingEnabled = true, analyticReportingEnabled = false))

    onNodeWithText("Developer analytics").performScrollTo().performClick()
    onNodeWithText("Usage analytics").performScrollTo().performClick()

    assertThat(events).containsExactly(
      AboutSettingEvent.CrashReportingEnabled(false),
      AboutSettingEvent.AnalyticReportingEnabled(true),
    )
  }

  @Test
  fun `the version row shows the build commit`() = runComposeUiTest {
    setAboutPane()

    // The time is in this machine's zone, so only the SHA is pinned here; VersionSettingTest covers the format.
    onNodeWithText("1.4.0 (1040099)").performScrollTo().assertIsDisplayed()
    onNodeWithText("(220c0be)", substring = true).assertIsDisplayed()
  }

  @Test
  fun `tapping version counts down to developer mode`() = runComposeUiTest {
    setAboutPane()

    repeat(DEVELOPER_MODE_CLICKS - 1) {
      onNodeWithText("Version").performScrollTo().performClick()
      waitForIdle()
    }
    assertThat(toasts.last()).isEqualTo("1 more clicks to enable developer mode!")
    assertThat(events).isEmpty()

    onNodeWithText("Version").performClick()
    waitForIdle()

    assertThat(events).containsExactly(SettingsUiEvent.DeveloperSettingEvent.EnableDeveloperMode)
    assertThat(toasts.last()).isEqualTo("Developer mode enabled!")
  }

  @Test
  fun `the tap count resets only after the timeout`() = runComposeUiTest {
    setAboutPane()

    onNodeWithText("Version").performScrollTo().performClick()
    waitForIdle()
    mainClock.advanceTimeBy(DEVELOPER_MODE_CLICK_TIMEOUT - 500)
    onNodeWithText("Version").performClick()
    waitForIdle()
    mainClock.advanceTimeBy(DEVELOPER_MODE_CLICK_TIMEOUT + 100)
    onNodeWithText("Version").performClick()
    waitForIdle()

    assertThat(toasts).containsExactly(
      "5 more clicks to enable developer mode!",
      // Within the timeout: still counting.
      "4 more clicks to enable developer mode!",
      // After it: back to the start.
      "5 more clicks to enable developer mode!",
    )
  }

  @Test
  fun `version taps do nothing once developer mode is on`() = runComposeUiTest {
    setAboutPane(developerModeEnabled = true)

    onNodeWithText("Version").performScrollTo().assertHasNoClickAction()
  }

  private fun ComposeUiTest.setAboutPane(
    aboutSettings: AboutSettingsInfo = AboutSettingsInfo(
      crashReportingEnabled = false,
      analyticReportingEnabled = false,
    ),
    developerModeEnabled: Boolean = false,
  ) {
    setContent {
      CampfireTheme {
        CompositionLocalProvider(
          LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
          LocalContentLayout provides ContentLayout.Root,
          LocalToast provides Toast { message, _ ->
            toasts += message
            ToastHandle {}
          },
        ) {
          AboutPane(
            aboutSettings = aboutSettings,
            applicationInfo = APPLICATION_INFO,
            developerModeEnabled = developerModeEnabled,
            eventSink = { events += it },
            onBackClick = {},
          )
        }
      }
    }
  }

  private companion object {
    val APPLICATION_INFO = ApplicationInfo(
      packageName = "app.campfire",
      debugBuild = false,
      flavor = Flavor.Standard,
      versionName = "1.4.0",
      versionCode = 1040099,
      osName = "Test",
      osVersion = "1",
      buildCommit = BuildCommit(Instant.parse("2026-11-24T14:03:27Z"), "220c0be"),
    )
  }
}
