// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test

class UserPrefixedSettingsTest {

  private val app = MapSettings()
  private val user1 = UserPrefixedSettings(app, "user-1")
  private val user2 = UserPrefixedSettings(app, "user-2")

  @Test
  fun `an account's values are stored under its own prefix`() {
    user1.putString(KEY_SORT_MODE, "title")

    assertThat(app.getStringOrNull("user:user-1:$KEY_SORT_MODE")).isEqualTo("title")
    assertThat(app.getStringOrNull(KEY_SORT_MODE)).isNull()
    assertThat(user1.getStringOrNull(KEY_SORT_MODE)).isEqualTo("title")
  }

  @Test
  fun `accounts don't see each other's values or the app's`() {
    app.putString(KEY_SORT_MODE, "duration")
    user1.putString(KEY_SORT_MODE, "title")

    assertThat(user2.getStringOrNull(KEY_SORT_MODE)).isNull()
    assertThat(user2.hasKey(KEY_SORT_MODE)).isFalse()
    assertThat(user1.hasKey(KEY_SORT_MODE)).isTrue()
  }

  @Test
  fun `keys and size only count the account's own settings`() {
    app.putString(KEY_THEME, "dark")
    user1.putString(KEY_SORT_MODE, "title")
    user1.putBoolean(KEY_SHOW_TIME_IN_BOOK, false)
    user2.putString(KEY_SORT_MODE, "duration")

    assertThat(user1.keys).containsOnly(KEY_SORT_MODE, KEY_SHOW_TIME_IN_BOOK)
    assertThat(user1.size).isEqualTo(2)
  }

  @Test
  fun `clearing an account leaves the app's and other accounts' settings`() {
    app.putString(KEY_THEME, "dark")
    user1.putString(KEY_SORT_MODE, "title")
    user2.putString(KEY_SORT_MODE, "duration")

    user1.clear()

    assertThat(user1.keys).containsOnly()
    assertThat(user2.getStringOrNull(KEY_SORT_MODE)).isEqualTo("duration")
    assertThat(app.getStringOrNull(KEY_THEME)).isEqualTo("dark")
  }

  @Test
  fun `listeners hear only the account's own key`() {
    val heard = mutableListOf<String?>()
    val listener = user1.addStringOrNullListener(KEY_SORT_MODE) { heard += it }

    app.putString(KEY_SORT_MODE, "duration")
    user2.putString(KEY_SORT_MODE, "author")
    user1.putString(KEY_SORT_MODE, "title")
    listener.deactivate()
    user1.putString(KEY_SORT_MODE, "added")

    assertThat(heard).isEqualTo(listOf<String?>("title"))
  }
}
