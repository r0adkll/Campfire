// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.common.test.user
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItem
import app.campfire.core.session.UserSession
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
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
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

class InProcessDiscoverScanTrackerTest {

  private val seriesRepository = FakeSeriesRepository()
  private val registry = FakeBookInfoRegistry()
  private val fatherTime = FakeFatherTime()
  private val session: UserSession = UserSession.LoggedIn(user(id = "user-1"))

  /** Shared across trackers to simulate persistence across process restarts. */
  private val settings = MapSettings()

  private fun store() = DiscoverScanStateStore(
    settings = settings,
    userSession = session,
    fatherTime = fatherTime,
  )

  private fun TestScope.tracker() = InProcessDiscoverScanTracker(
    seriesScanner = SeriesScanner(seriesRepository, registry),
    store = store(),
    coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
  )

  private fun ownedItem(title: String, asin: String): LibraryItem = libraryItem(
    media = media(metadata = mediaMetadata(title = title, ASIN = asin)),
  )

  private fun success() = SeriesFetchResult.Success(
    SeriesInfoState(
      providerId = ProviderId.Audible,
      providerName = "Audible",
      isCompleted = null,
      entries = emptyList(),
    ),
  )

  private suspend fun InProcessDiscoverScanTracker.awaitCompleted(): DiscoverScanState.Completed {
    return state.first { it is DiscoverScanState.Completed } as DiscoverScanState.Completed
  }

  private suspend fun emitOneScannableSeries() {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    registry.fetchSeriesResults["Series One"] = success()
  }

  @Test
  fun `a scan completes with counts and persists them`() = runTest {
    val untracked = libraryItem(
      media = media(metadata = mediaMetadata(title = "Untracked", ISBN = null, ASIN = null)),
    )
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Mystery Series", books = listOf(untracked)),
      ),
    )
    registry.fetchSeriesResults["Series One"] = success()
    val tracker = tracker()

    tracker.startScan()

    val completed = tracker.awaitCompleted()
    assertThat(completed.skippedCount).isEqualTo(1)
    assertThat(completed.failedCount).isEqualTo(0)
    assertThat(store().lastCompleted()).isEqualTo(completed)
  }

  @Test
  fun `a new tracker rehydrates the persisted completion`() = runTest {
    emitOneScannableSeries()
    val first = tracker()
    first.startScan()
    val completed = first.awaitCompleted()

    val second = tracker()

    assertThat(second.state.value).isEqualTo(completed)
  }

  @Test
  fun `the freshness window holds across restarts`() = runTest {
    emitOneScannableSeries()
    val first = tracker()
    first.startScanIfStale()
    first.awaitCompleted()

    // A "relaunch" within the window keeps the persisted results untouched.
    fatherTime.nowMillis += 30.minutes.inWholeMilliseconds
    val second = tracker()
    second.startScanIfStale()
    runCurrent()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(1)
    assertThat(second.state.value).isInstanceOf(DiscoverScanState.Completed::class)
  }

  @Test
  fun `opening after the freshness window rescans`() = runTest {
    emitOneScannableSeries()
    val first = tracker()
    first.startScanIfStale()
    first.awaitCompleted()

    fatherTime.nowMillis += 2.hours.inWholeMilliseconds
    val second = tracker()
    second.startScanIfStale()
    second.state.first { it is DiscoverScanState.Running }
    second.awaitCompleted()

    assertThat(registry.fetchSeriesRequests.size).isEqualTo(2)
  }

  @Test
  fun `cancelling freezes progress into a completed state`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    seriesRepository.allSeriesFlow.emit(
      listOf(
        series(id = "s1", name = "Series One", books = listOf(ownedItem("Book A", "B000000001"))),
        series(id = "s2", name = "Series Two", books = listOf(ownedItem("Book B", "B000000002"))),
      ),
    )
    registry.fetchSeriesResults["Series One"] = success()
    val tracker = tracker()

    tracker.startScan()
    gate.send(Unit)
    tracker.state.first { it is DiscoverScanState.Running && it.done == 1 }

    tracker.cancelScan()

    val completed = tracker.awaitCompleted()
    assertThat(completed.failedCount).isEqualTo(0)
    assertThat(store().lastCompleted()).isEqualTo(completed)
  }

  @Test
  fun `starting a scan while one runs is a no-op`() = runTest {
    val gate = Channel<Unit>()
    registry.fetchSeriesGate = { gate.receive() }
    emitOneScannableSeries()
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
    emitOneScannableSeries()
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
  fun `a rate limited stop freezes as completed with the flag set`() = runTest {
    seriesRepository.allSeriesFlow.emit(
      listOf(series(id = "s1", name = "Limited", books = listOf(ownedItem("Book A", "B000000001")))),
    )
    registry.fetchSeriesResults["Limited"] = SeriesFetchResult.RateLimited(retryAfter = null)
    val tracker = tracker()

    tracker.startScan()

    val completed = tracker.awaitCompleted()
    assertThat(completed.rateLimited).isTrue()
    assertThat(store().lastCompleted()!!.rateLimited).isTrue()
  }
}
