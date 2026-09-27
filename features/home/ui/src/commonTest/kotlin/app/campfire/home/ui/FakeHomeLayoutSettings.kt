// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import app.campfire.core.model.LibraryId
import app.campfire.home.api.HomeLayoutSettings
import app.campfire.home.api.model.HomeLayoutEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHomeLayoutSettings : HomeLayoutSettings {

  val layouts = MutableStateFlow<Map<LibraryId, List<HomeLayoutEntry>>>(emptyMap())

  override fun observeLayout(libraryId: LibraryId): Flow<List<HomeLayoutEntry>?> {
    return layouts.map { it[libraryId] }
  }

  override fun setLayout(libraryId: LibraryId, entries: List<HomeLayoutEntry>) {
    layouts.value += libraryId to entries
  }

  override fun resetLayout(libraryId: LibraryId) {
    layouts.value -= libraryId
  }
}
