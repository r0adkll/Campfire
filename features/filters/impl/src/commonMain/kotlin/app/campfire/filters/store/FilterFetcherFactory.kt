// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.filters.store

import app.campfire.core.model.LibraryId
import app.campfire.crashreporting.CrashReporter
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.FilterData
import org.mobilenativefoundation.store.store5.Fetcher

class FilterFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<LibraryId, FilterData> = Fetcher.ofResult { libraryId ->
    api.getFilterData(libraryId)
      .onSuccess { filterData ->
        CrashReporter.tag("bookCount", filterData.bookCount.toString())
      }
      .asFetcherResult()
  }
}
