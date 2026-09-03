// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItem
import app.campfire.core.time.FatherTime
import app.campfire.discover.api.DiscoverScanState
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.home.ui.series
import app.campfire.series.test.FakeSeriesRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private class FakeFatherTime(var nowMillis: Long = 0L) : FatherTime {
  override fun now(): LocalDateTime =
    Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC)

  override fun today(): LocalDate = now().date
  override fun nowInEpochMillis(): Long = nowMillis
}

class DefaultDiscoverScanTrackerTest {

  private val seriesRepository = FakeSeriesRepository()
  private val registry = FakeBookInfoRegistry()
  private val fatherTime = FakeFatherTime()

  private fun TestScope.tracker() = DefaultDiscoverScanTracker(
    seriesRepository = seriesRepository,
    bookInfoRegistry = registry,
    fatherTime = fatherTime,
    coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
  )

  private fun ownedItem(title: String, asin: String): LibraryItem = libraryItem(
    media = media(metadata = mediaMetadata(title = title, ASIN = asin)),
  )

  private fun providerEntry(id: String, title: String, released: Boolean) = ProviderSeriesEntry(
    providerBookId = id,
    position = 2.0,
    title = title,
    releaseDate = if (released) "2014-03-04" else "2031-01-01",
    isReleased = released,
    providerUrl = "https://audible.com/pd/$id",
    coverUrl = null,
  )

  private fun successWith(vararg entries: SeriesEntry) = SeriesFetchResult.Success(
    SeriesInfoState(
      providerId = ProviderId.Audible,
      providerName = "Audible",
      isCompleted = null,
      entries = entries.toList(),
    ),
  )

  private suspend fun DefaultDiscoverScanTracker.awaitCompleted(): DiscoverScanState.Completed {
    return state.first { it is DiscoverScanState.Completed } as DiscoverScanState.Completed
  }

  @Test
  fun `a scan collects only the unreleased provider entries`() = runTest {
    val owned = ownedItem("The Way of Kings", "B003P2WO5E")
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "The Stormlight Archive", books = listOf(owned))),
    )
    registry.fetchSeriesResults["The Stormlight Archive"] = successWith(
      SeriesEntry.Owned(owned),
      SeriesEntry.Missing(providerEntry("B00BWWSVPU", "Words of Radiance", released = true), ProviderId.Audible),
      SeriesEntry.Upcoming(providerEntry("B0UPCOMING", "Untitled #6", released = false), ProviderId.Audible),
    )
    val tracker = tracker()

    tracker.startScan()

    val state = tracker.awaitCompleted()
    assertThat(state.results.providerName).isEqualTo("Audible")
    // Owned and missing entries are the series page's concern — only upcoming collects.
    assertThat(state.results.upcoming.map { it.entry.title }).isEqualTo(listOf("Untitled #6"))
    assertThat(state.results.upcoming.single().seriesId).isEqualTo("s1")
    assertThat(state.results.upcoming.single().seriesName).isEqualTo("The Stormlight Archive")
    assertThat(state.skippedCount).isEqualTo(0)
    assertThat(state.failedCount).isEqualTo(0)
  }

  @Test
  fun `unscannable series are skipped without a registry call`() = runTest {
    val untracked = libraryItem(
      media = media(metadata = mediaMetadata(title = "Untracked", ISBN = null, ASIN = null)),
    )
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Mystery Series", books = listOf(untracked))),
    )
    val tracker = tracker()

    tracker.startScan()

    val state = tracker.awaitCompleted()
    assertThat(state.skippedCount).isEqualTo(1)
    assertThat(registry.fetchSeriesRequests.size).isEqualTo(0)
  }

  @Test
  fun `failed fetches are counted while successful ones still report`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Broken Series", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Good Series", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Broken Series"] = SeriesFetchResult.Error
    registry.fetchSeriesResults["Good Series"] = successWith(
      SeriesEntry.Upcoming(providerEntry("B000000003", "Book C", released = false), ProviderId.Audible),
    )
    val tracker = tracker()

    tracker.startScan()

    val state = tracker.awaitCompleted()
    assertThat(state.failedCount).isEqualTo(1)
    assertThat(state.results.upcoming.map { it.entry.title }).isEqualTo(listOf("Book C"))
  }

  @Test
  fun `progress streams in while the scan runs`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Series Two", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Series One"] = successWith(
      SeriesEntry.Upcoming(providerEntry("B000000003", "Book C", released = false), ProviderId.Audible),
    )
    registry.fetchSeriesResults["Series Two"] = successWith()
    val tracker = tracker()

    tracker.startScan()
    val initial = tracker.state.first { it is DiscoverScanState.Running } as DiscoverScanState.Running
    assertThat(initial.total).isEqualTo(2)

    gate.send(Unit)
    val partial = tracker.state
      .first { it is DiscoverScanState.Running && it.done == 1 } as DiscoverScanState.Running
    assertThat(partial.results.upcoming.map { it.entry.title }).isEqualTo(listOf("Book C"))

    gate.send(Unit)
    assertThat(tracker.awaitCompleted().results.upcoming.size).isEqualTo(1)
  }

  @Test
  fun `cancelling freezes partial results into a completed state`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Series Two", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Series One"] = successWith(
      SeriesEntry.Upcoming(providerEntry("B000000003", "Book C", released = false), ProviderId.Audible),
    )
    val tracker = tracker()

    tracker.startScan()
    gate.send(Unit)
    tracker.state.first { it is DiscoverScanState.Running && it.done == 1 }

    tracker.cancelScan()

    val state = tracker.awaitCompleted()
    assertThat(state.results.upcoming.map { it.entry.title }).isEqualTo(listOf("Book C"))
  }

  @Test
  fun `starting a scan while one runs is a no-op`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    val tracker = tracker()

    tracker.startScan()
    tracker.state.first { it is DiscoverScanState.Running }
    runCurrent()
    tracker.startScan()
    runCurrent()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(1)
    gate.send(Unit)
    tracker.awaitCompleted()
  }

  @Test
  fun `an explicit scan refreshes while an open scan serves the cache`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    val tracker = tracker()

    tracker.startScanIfStale()
    tracker.awaitCompleted()
    tracker.startScan()
    tracker.state.first { it is DiscoverScanState.Running }
    tracker.awaitCompleted()

    assertThat(registry.fetchSeriesRequests.map { it.refresh }).isEqualTo(listOf(false, true))
    // The stale scan reads the local snapshot; only the explicit scan
    // refreshes the series listing itself.
    assertThat(seriesRepository.observeAllSeriesRequests).isEqualTo(listOf(false, true))
  }

  @Test
  fun `opening with fresh results keeps them instead of rescanning`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    val tracker = tracker()

    tracker.startScanIfStale()
    val completed = tracker.awaitCompleted()

    fatherTime.nowMillis += 30.minutes.inWholeMilliseconds
    tracker.startScanIfStale()
    runCurrent()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(1)
    assertThat(tracker.state.value).isEqualTo(completed)
  }

  @Test
  fun `opening after the freshness window rescans`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    val tracker = tracker()

    tracker.startScanIfStale()
    tracker.awaitCompleted()

    fatherTime.nowMillis += 2.hours.inWholeMilliseconds
    tracker.startScanIfStale()
    tracker.state.first { it is DiscoverScanState.Running }
    tracker.awaitCompleted()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(2)
  }

  @Test
  fun `a rescan runs the whole scan again`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    val tracker = tracker()

    tracker.startScan()
    tracker.awaitCompleted()
    tracker.startScan()
    tracker.state.first { it is DiscoverScanState.Running }
    tracker.awaitCompleted()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(2)
    assertThat(tracker.state.value).isInstanceOf(DiscoverScanState.Completed::class)
  }
}
