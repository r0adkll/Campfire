// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.filters.test

import app.campfire.core.model.FilterData
import app.campfire.core.model.LibraryId
import app.campfire.filters.FilteringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFilteringRepository : FilteringRepository {

  var selectedLibraryId: LibraryId = "library"

  /** The filter data of each library; one without any has none */
  val filterData = MutableStateFlow<Map<LibraryId, FilterData>>(emptyMap())

  override fun observeFilterData(): Flow<FilterData> {
    return filterData.map { it[selectedLibraryId] ?: FilterData() }
  }

  val observedLibraries = mutableListOf<LibraryId>()
  override fun observeFilterData(libraryId: LibraryId): Flow<FilterData> {
    observedLibraries += libraryId
    return filterData.map { it[libraryId] ?: FilterData() }
  }
}
