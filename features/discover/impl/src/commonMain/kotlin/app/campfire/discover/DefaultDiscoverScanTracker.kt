// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.seriesMatch
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.Series
import app.campfire.core.time.FatherTime
import app.campfire.discover.api.DiscoverScanResults
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoverScanTracker
import app.campfire.discover.api.DiscoveredBook
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import me.tatarka.inject.annotations.Inject

/**
 * Providers resolve a cold series in a few catalog requests, so keep the fan-out
 * narrow — this is also the only politeness mechanism toward Audible, which has
 * no client-side throttle.
 */
private const val SCAN_CONCURRENCY = 2

/**
 * How long a completed scan counts as fresh for [startScanIfStale] — matched
 * to the registry's series miss TTL, since a sooner rescan couldn't surface
 * anything new anyway.
 */
private val SCAN_FRESHNESS_TTL = 1.hours

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultDiscoverScanTracker(
  private val seriesRepository: SeriesRepository,
  private val bookInfoRegistry: BookInfoRegistry,
  private val fatherTime: FatherTime,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : DiscoverScanTracker {

  private val _state = MutableStateFlow<DiscoverScanState>(DiscoverScanState.Idle)
  override val state: StateFlow<DiscoverScanState> = _state.asStateFlow()

  private var scanJob: Job? = null

  override fun startScan() {
    // An explicit scan is the user asking for current data: bypass the series
    // cache and refetch every listing from the provider.
    launchScan(refresh = true)
  }

  override fun startScanIfStale() {
    val completed = _state.value as? DiscoverScanState.Completed
    val isFresh = completed != null &&
      fatherTime.nowInEpochMillis() - completed.scannedAt.toEpochMilliseconds() <
      SCAN_FRESHNESS_TTL.inWholeMilliseconds
    if (!isFresh) launchScan(refresh = false)
  }

  override fun cancelScan() {
    scanJob?.cancel()
  }

  private fun launchScan(refresh: Boolean) {
    if (scanJob?.isActive == true) return
    scanJob = coroutineScopeHolder.get().launch { scan(refresh) }
  }

  private suspend fun scan(refresh: Boolean) {
    // Stale scans read the local snapshot so they never rewrite the series
    // cache (which would reset the series list screen's pagination); an
    // explicit scan also kicks off a listing refresh so new series land for
    // this or the next pass.
    val allSeries = seriesRepository.observeAllSeries(refresh = refresh).first()
    val outcomes = arrayOfNulls<Outcome>(allSeries.size)
    val mutex = Mutex()
    _state.value = DiscoverScanState.Running(0, allSeries.size, DiscoverScanResults())

    try {
      val semaphore = Semaphore(SCAN_CONCURRENCY)
      coroutineScope {
        allSeries.forEachIndexed { index, series ->
          launch {
            semaphore.withPermit {
              val outcome = scanSeries(series, refresh)
              mutex.withLock {
                outcomes[index] = outcome
                _state.value = DiscoverScanState.Running(
                  done = outcomes.count { it != null },
                  total = outcomes.size,
                  results = outcomes.accumulate(),
                )
              }
            }
          }
        }
      }
    } finally {
      // Runs for both completion and cancellation — a cancelled scan freezes
      // whatever it found so partial results stay usable.
      _state.value = DiscoverScanState.Completed(
        results = outcomes.accumulate(),
        scannedAt = Instant.fromEpochMilliseconds(fatherTime.nowInEpochMillis()),
        skippedCount = outcomes.count { it == Outcome.Skipped },
        failedCount = outcomes.count { it == Outcome.Failed },
      )
    }
  }

  private suspend fun scanSeries(series: Series, refresh: Boolean): Outcome {
    val owned = series.books.orEmpty()
    // Pre-check identifiability so unscannable series don't cost a fetch.
    if (seriesMatch(series.name, owned) == null) return Outcome.Skipped

    return when (val result = bookInfoRegistry.fetchSeriesEntries(series.name, owned, refresh)) {
      is SeriesFetchResult.Success -> Outcome.Books(
        providerName = result.state.providerName,
        upcoming = result.state.entries
          .filterIsInstance<SeriesEntry.Upcoming>()
          .map { DiscoveredBook(series.id, series.name, it.entry, it.providerId) },
      )

      SeriesFetchResult.Unavailable -> Outcome.Skipped
      SeriesFetchResult.Error -> Outcome.Failed
    }
  }

  /** Folds finished outcomes in series order, so streaming results are stable. */
  private fun Array<Outcome?>.accumulate(): DiscoverScanResults {
    val books = filterIsInstance<Outcome.Books>()
    return DiscoverScanResults(
      providerName = books.firstNotNullOfOrNull { it.providerName },
      upcoming = books.flatMap { it.upcoming },
    )
  }

  private sealed interface Outcome {
    data class Books(
      val providerName: String?,
      val upcoming: List<DiscoveredBook>,
    ) : Outcome

    data object Skipped : Outcome
    data object Failed : Outcome
  }
}
