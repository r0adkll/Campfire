// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.store

import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Series as NetworkSeries
import app.campfire.network.nextPage
import org.mobilenativefoundation.store.store5.Fetcher

private const val PAGE_LIMIT = 500

internal class SeriesFetcherFactory(val api: AudioBookShelfApi) {

  fun create() = Fetcher.ofResult { key: SeriesStore.Key ->
    fetchAllPages(key.libraryId).asFetcherResult()
  }

  /**
   * The store's value is the library's complete series listing, so walk every
   * page rather than trusting a single oversized request to cover it.
   */
  private suspend fun fetchAllPages(libraryId: String): Result<List<NetworkSeries>> {
    val series = mutableListOf<NetworkSeries>()
    var page = 0
    while (true) {
      val response = api.getSeries(libraryId, page = page, limit = PAGE_LIMIT)
        .getOrElse { return Result.failure(it) }
      series += response.data
      // An empty page can't advance the offset; stop rather than loop on a short total.
      page = response.nextPage?.takeIf { response.data.isNotEmpty() } ?: return Result.success(series)
    }
  }
}
