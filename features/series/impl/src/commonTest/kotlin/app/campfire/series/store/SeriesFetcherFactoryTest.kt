// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.store

import app.campfire.network.PagedResponse
import app.campfire.network.models.Series
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.network.test.model.createNetworkSeries
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.store.store5.FetcherResult

class SeriesFetcherFactoryTest {

  private val api = FakeAudioBookShelfApi()
  private val fetcher = SeriesFetcherFactory(api).create()
  private val key = SeriesStore.Key(userId = "user", libraryId = "library")

  private fun pageOf(page: Int, ids: List<String>, total: Int, limit: Int = 2) = PagedResponse(
    data = ids.map { createNetworkSeries(it) },
    page = page,
    limit = limit,
    total = total,
    offset = page * limit,
  )

  private suspend fun fetch(): FetcherResult<List<Series>> = fetcher(key).first()

  @Test
  fun `walks every page of the listing`() = runTest {
    api.seriesResult = { page ->
      when (page) {
        0 -> Result.success(pageOf(0, listOf("a", "b"), total = 5))
        1 -> Result.success(pageOf(1, listOf("c", "d"), total = 5))
        else -> Result.success(pageOf(2, listOf("e"), total = 5))
      }
    }

    val result = fetch()

    assertThat(api.seriesPageRequests).containsExactly(0, 1, 2)
    assertThat(result).isInstanceOf<FetcherResult.Data<List<Series>>>()
    val ids = (result as FetcherResult.Data).value.map { it.id }
    assertThat(ids).containsExactly("a", "b", "c", "d", "e")
  }

  @Test
  fun `a failed page fails the whole listing`() = runTest {
    api.seriesResult = { page ->
      when (page) {
        0 -> Result.success(pageOf(0, listOf("a", "b"), total = 4))
        else -> Result.failure(IllegalStateException("boom"))
      }
    }

    assertThat(fetch()).isInstanceOf<FetcherResult.Error.Exception>()
  }

  @Test
  fun `an empty page ends the listing even when the total says otherwise`() = runTest {
    api.seriesResult = { page ->
      when (page) {
        0 -> Result.success(pageOf(0, listOf("a", "b"), total = 10))
        else -> Result.success(pageOf(page, emptyList(), total = 10))
      }
    }

    val result = fetch()

    assertThat(api.seriesPageRequests).containsExactly(0, 1)
    val ids = (result as FetcherResult.Data).value.map { it.id }
    assertThat(ids).containsExactly("a", "b")
  }
}
