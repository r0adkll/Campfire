// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.navigator

import app.campfire.common.screens.UrlScreen
import app.campfire.common.screens.WindowScreen
import app.campfire.reader.api.screen.ReaderScreen
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import kotlin.test.Test

class WindowScreenInterceptorTest {

  private val launched = mutableListOf<WindowScreen>()
  private val interceptor = WindowScreenInterceptor { launched += it }

  @Test
  fun `window screens open in their own window`() {
    val screen = ReaderScreen(libraryItemId = "book")

    val result = interceptor.goTo(screen)

    assertThat(result).isEqualTo(NavigationInterceptor.SuccessConsumed)
    assertThat(launched).containsExactly(screen)
  }

  @Test
  fun `other screens are left to the back stack`() {
    val result = interceptor.goTo(UrlScreen("https://example.com"))

    assertThat(result).isEqualTo(NavigationInterceptor.Skipped)
    assertThat(launched).isEmpty()
  }
}
