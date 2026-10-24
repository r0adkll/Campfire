// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.navigator

import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.management.api.screen.ItemEditorScreen
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** Workspaces take the main area, with what they're about in the pane beside them. */
class WorkspaceNavigationTest {

  private val root = FakeNavigator(UrlScreen("root"))
  private val pane = FakeNavigator(EmptyScreen("pane"))

  @Test
  fun `a workspace opened from the pane goes to the main area`() = runTest {
    val interceptor = WorkspaceScreenInterceptor(root)

    val result = interceptor.goTo(ItemEditorScreen("book"))

    assertThat(result).isEqualTo(NavigationInterceptor.SuccessConsumed)
    assertThat(root.awaitNextScreen()).isEqualTo(ItemEditorScreen("book"))
  }

  @Test
  fun `the pane's other screens stay in the pane`() {
    val interceptor = WorkspaceScreenInterceptor(root)

    assertThat(interceptor.goTo(LibraryItemScreen("book"))).isEqualTo(NavigationInterceptor.Skipped)
  }

  @Test
  fun `on a wide window a workspace previews its item in the pane`() = runTest {
    val navigator = HomeNavigator(Wide, rootNavigator = root, detailNavigator = pane)

    navigator.goTo(ItemEditorScreen("book"))

    assertThat(root.awaitNextScreen()).isEqualTo(ItemEditorScreen("book"))
    assertThat(pane.awaitNextScreen()).isEqualTo(LibraryItemScreen("book"))
  }

  @Test
  fun `a preview the pane already shows isn't opened again`() = runTest {
    val pane = FakeNavigator(LibraryItemScreen("book"))
    val navigator = HomeNavigator(Wide, rootNavigator = root, detailNavigator = pane)

    navigator.goTo(ItemEditorScreen("book"))

    assertThat(root.awaitNextScreen()).isEqualTo(ItemEditorScreen("book"))
    pane.expectNoEvents()
    assertThat(pane.peekBackStack()).containsExactly(LibraryItemScreen("book"))
  }

  @Test
  fun `on a narrow window a workspace only opens in the main area`() = runTest {
    val navigator = HomeNavigator(Narrow, rootNavigator = root, detailNavigator = pane)

    navigator.goTo(ItemEditorScreen("book"))

    assertThat(root.awaitNextScreen()).isEqualTo(ItemEditorScreen("book"))
    pane.expectNoEvents()
  }

  private companion object {
    val Wide = WindowSizeClass(minWidthDp = 1200, minHeightDp = 800)
    val Narrow = WindowSizeClass(minWidthDp = 400, minHeightDp = 800)
  }
}
