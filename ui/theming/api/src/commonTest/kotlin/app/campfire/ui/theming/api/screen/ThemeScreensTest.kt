// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.screen

import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test

class ThemeScreensTest {

  /** The theme screens draw their own previews, which the playback bar would cover. */
  @Test
  fun themeScreens_hideThePlaybackBarAndBottomNav() {
    listOf(ThemePickerScreen, ThemeBuilderScreen(), AiThemeBuilderScreen()).forEach { screen ->
      assertThat(screen.presentation.hidePlaybackBar, name = "${screen.name} hides the playback bar").isTrue()
      assertThat(screen.presentation.hideBottomNav, name = "${screen.name} hides the bottom nav").isTrue()
    }
  }
}
