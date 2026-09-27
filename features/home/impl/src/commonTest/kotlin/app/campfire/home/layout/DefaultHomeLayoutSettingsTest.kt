// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.layout

import app.campfire.core.session.UserSession
import app.campfire.home.api.model.HomeLayoutEntry
import app.campfire.user.test.fixtures.user
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DefaultHomeLayoutSettingsTest {

  private val backing = MapSettings()
  private val settings = settingsFor("user")

  private val layout = listOf(
    HomeLayoutEntry("discover", visible = true, label = "Discover"),
    HomeLayoutEntry("recent-series", visible = false, label = "Recent Series"),
  )

  @Test
  fun `a library without a saved layout has none`() = runTest {
    assertThat(settings.observeLayout("library").first()).isNull()
  }

  @Test
  fun `a saved layout reads back`() = runTest {
    settings.setLayout("library", layout)

    assertThat(settings.observeLayout("library").first()).isEqualTo(layout)
  }

  @Test
  fun `layouts are per library and per user`() = runTest {
    settings.setLayout("library", layout)

    assertThat(settings.observeLayout("other-library").first()).isNull()
    assertThat(settingsFor("other-user").observeLayout("library").first()).isNull()
  }

  @Test
  fun `observers see edits and resets`() = runTest {
    settings.observeLayout("library").test {
      assertThat(awaitItem()).isNull()

      settings.setLayout("library", layout)
      assertThat(awaitItem()).isEqualTo(layout)

      settings.resetLayout("library")
      assertThat(awaitItem()).isNull()
    }
  }

  @Test
  fun `an unreadable layout falls back to none`() = runTest {
    backing.putString("home_layout_user_library", "not json")

    assertThat(settings.observeLayout("library").first()).isNull()
  }

  @Test
  fun `stored entries keep their order`() = runTest {
    settings.setLayout("library", layout.reversed())

    assertThat(settings.observeLayout("library").first().orEmpty().map { it.shelfId })
      .containsExactly("recent-series", "discover")
  }

  private fun settingsFor(userId: String) = DefaultHomeLayoutSettings(
    settings = backing,
    userSession = UserSession.LoggedIn(user(userId)),
  )
}
