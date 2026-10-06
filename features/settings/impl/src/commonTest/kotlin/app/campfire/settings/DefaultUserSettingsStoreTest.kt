// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class DefaultUserSettingsStoreTest {

  private val app = MapSettings(
    KEY_SORT_MODE to ContentSortMode.Duration.storageKey,
    KEY_LIBRARY_ITEM_DISPLAY_STATE to ItemDisplayState.List.storageKey,
    KEY_SHOW_TIME_IN_BOOK to false,
    KEY_THEME to "dark",
  )
  private val store = DefaultUserSettingsStore(app)

  private fun libraryView(userId: String?) = LibraryViewSettingsImpl(
    settings = store.settingsFor(userId),
    scopeHolder = CoroutineScopeHolder { CoroutineScope(Dispatchers.Unconfined + Job()) },
  )

  @Test
  fun `an account starts from the library view the app kept for everyone`() {
    val settings = libraryView("user-1")

    assertThat(settings.librarySortMode).isEqualTo(ContentSortMode.Duration)
    assertThat(settings.libraryItemDisplayState).isEqualTo(ItemDisplayState.List)
    assertThat(settings.showTimeInBook).isFalse()
  }

  @Test
  fun `only library view settings are copied into an account`() {
    store.settingsFor("user-1")

    assertThat(UserPrefixedSettings(app, "user-1").keys).containsOnly(
      KEY_SORT_MODE,
      KEY_LIBRARY_ITEM_DISPLAY_STATE,
      KEY_SHOW_TIME_IN_BOOK,
      KEY_SEEDED,
    )
  }

  @Test
  fun `an account's changes are kept when its settings are opened again`() {
    libraryView("user-1").librarySortMode = ContentSortMode.Title

    assertThat(libraryView("user-1").librarySortMode).isEqualTo(ContentSortMode.Title)
  }

  @Test
  fun `accounts keep their library view apart`() {
    libraryView("user-1").librarySortMode = ContentSortMode.Title

    assertThat(libraryView("user-2").librarySortMode).isEqualTo(ContentSortMode.Duration)
    assertThat(app.getStringOrNull(KEY_SORT_MODE)).isEqualTo(ContentSortMode.Duration.storageKey)
  }

  @Test
  fun `signed out, the settings are the app-wide ones`() {
    assertThat(store.settingsFor(null)).isSameInstanceAs(app)
  }

  @Test
  fun `clearing an account starts it over from the app-wide values`() {
    libraryView("user-1").librarySortMode = ContentSortMode.Title
    libraryView("user-2").librarySortMode = ContentSortMode.AuthorFL

    store.clear("user-1")

    assertThat(UserPrefixedSettings(app, "user-1").keys).containsOnly()
    assertThat(libraryView("user-1").librarySortMode).isEqualTo(ContentSortMode.Duration)
    assertThat(libraryView("user-2").librarySortMode).isEqualTo(ContentSortMode.AuthorFL)
  }
}
