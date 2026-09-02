// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.core.model.LibraryItem
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.home.ui.series
import app.campfire.series.test.FakeSeriesRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class SeriesScannerTest {

  private val seriesRepository = FakeSeriesRepository()
  private val registry = FakeBookInfoRegistry()

  private val scanner = SeriesScanner(
    seriesRepository = seriesRepository,
    bookInfoRegistry = registry,
  )

  private fun ownedItem(title: String, asin: String): LibraryItem = libraryItem(
    media = media(metadata = mediaMetadata(title = title, ASIN = asin)),
  )

  private fun successWith(vararg entries: SeriesEntry) = SeriesFetchResult.Success(
    SeriesInfoState(
      providerId = ProviderId.Audible,
      providerName = "Audible",
      isCompleted = null,
      entries = entries.toList(),
    ),
  )

  private fun upcomingEntry(id: String, title: String) = SeriesEntry.Upcoming(
    ProviderSeriesEntry(
      providerBookId = id,
      position = 2.0,
      title = title,
      releaseDate = "2031-01-01",
      isReleased = false,
      providerUrl = "https://audible.com/pd/$id",
      coverUrl = null,
    ),
    ProviderId.Audible,
  )

  @Test
  fun `a scan counts scanned, skipped, and failed series`() = runTest {
    val untracked = libraryItem(
      media = media(metadata = mediaMetadata(title = "Untracked", ISBN = null, ASIN = null)),
    )
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Good Series", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Mystery Series", books = listOf(untracked)),
        series(id = "s3", name = "Broken Series", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Good Series"] = successWith(upcomingEntry("B000000003", "Book C"))
    registry.fetchSeriesResults["Broken Series"] = SeriesFetchResult.Error

    val terminal = scanner.scan(refresh = false).last()

    assertThat(terminal.done).isEqualTo(3)
    assertThat(terminal.total).isEqualTo(3)
    assertThat(terminal.skippedCount).isEqualTo(1)
    assertThat(terminal.failedCount).isEqualTo(1)
    assertThat(terminal.rateLimited).isFalse()
    // The identifiability pre-check spares the unscannable series a fetch.
    assertThat(registry.fetchSeriesRequests.size).isEqualTo(2)
  }

  @Test
  fun `the refresh flag flows through to the snapshot and every fetch`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    registry.fetchSeriesResults["Series One"] = successWith()

    scanner.scan(refresh = false).last()
    scanner.scan(refresh = true).last()

    assertThat(seriesRepository.observeAllSeriesRequests).isEqualTo(listOf(false, true))
    assertThat(registry.fetchSeriesRequests.map { it.refresh }).isEqualTo(listOf(false, true))
  }

  @Test
  fun `progress streams in as each series completes`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Series Two", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Series One"] = successWith()
    registry.fetchSeriesResults["Series Two"] = successWith()

    val emissions = mutableListOf<SeriesScanner.Progress>()
    val job = launch { scanner.scan(refresh = false).toList(emissions) }

    gate.send(Unit)
    gate.send(Unit)
    job.join()

    assertThat(emissions.map { it.done }).isEqualTo(listOf(0, 1, 2))
    assertThat(emissions.map { it.total }).isEqualTo(listOf(2, 2, 2))
  }

  @Test
  fun `recently updated series scan first`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Dusty", updatedAt = 100L, books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Active", updatedAt = 300L, books = listOf(ownedItem("Book B", "B000000002"))),
        series(id = "s3", name = "Recent", updatedAt = 200L, books = listOf(ownedItem("Book C", "B000000003"))),
      ),
    )

    scanner.scan(refresh = false).last()

    // An interrupted scan should have already covered what the user is
    // actively collecting, so the snapshot fans out most-recent first.
    assertThat(registry.fetchSeriesRequests.map { it.seriesName })
      .isEqualTo(listOf("Active", "Recent", "Dusty"))
  }

  @Test
  fun `a rate limited series is retried after the pause`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    registry.fetchSeriesResultQueue["Series One"] = ArrayDeque(
      listOf(
        SeriesFetchResult.RateLimited(retryAfter = 30.seconds),
        successWith(upcomingEntry("B000000003", "Book C")),
      ),
    )

    val terminal = scanner.scan(refresh = false).last()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(2)
    assertThat(terminal.done).isEqualTo(1)
    assertThat(terminal.failedCount).isEqualTo(0)
    assertThat(terminal.rateLimited).isFalse()
  }

  @Test
  fun `a second rate limit stops the scan with a terminal flag`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Limited", updatedAt = 300L, books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Also Limited", updatedAt = 100L, books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Limited"] = SeriesFetchResult.RateLimited(retryAfter = null)
    registry.fetchSeriesResults["Also Limited"] = SeriesFetchResult.RateLimited(retryAfter = null)

    val terminal = scanner.scan(refresh = false).last()

    assertThat(terminal.rateLimited).isTrue()
    // The stopped series isn't recorded as a failure — the caller resumes
    // from the cache checkpoint later.
    assertThat(terminal.failedCount).isEqualTo(0)
  }
}
