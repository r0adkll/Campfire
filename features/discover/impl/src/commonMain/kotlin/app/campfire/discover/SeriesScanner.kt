// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.seriesMatch
import app.campfire.core.model.Series
import app.campfire.series.api.SeriesRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import me.tatarka.inject.annotations.Inject

/**
 * Providers resolve a cold series in a few catalog requests, so keep the
 * fan-out narrow and let the provider's client-side throttle set the pace.
 */
private const val SCAN_CONCURRENCY = 2

/** Pause before retrying a rate-limited series when the provider gave no Retry-After. */
private val RATE_LIMIT_PAUSE = 60.seconds

/**
 * The scan core shared by every platform's [app.campfire.discover.api.DiscoverScanTracker]:
 * walks the cached series snapshot and asks the registry for each series'
 * canonical listing, emitting counter-only progress. Results aren't
 * accumulated here — every fetch lands in the registry's series cache, which
 * `BookInfoRegistry.observeCachedUpcoming` serves live to the UI.
 *
 * The snapshot is scanned most-recently-updated first, so an interrupted scan
 * (process death, background quota, rate limit) has already covered the series
 * the user is actively collecting.
 *
 * When the provider rate limits a fetch the scan pauses for the advertised
 * interval and retries that series once; a second refusal stops the scan with
 * [Progress.rateLimited] set so callers can persist the checkpoint and resume
 * later instead of recording thousands of spurious failures.
 */
@Inject
class SeriesScanner(
  private val seriesRepository: SeriesRepository,
  private val bookInfoRegistry: BookInfoRegistry,
) {

  data class Progress(
    val done: Int,
    val total: Int,
    val skippedCount: Int,
    val failedCount: Int,
    /** The provider rate limited the scan and it stopped early; this is the terminal emission. */
    val rateLimited: Boolean = false,
  )

  /**
   * Runs one full scan, emitting [Progress] after the initial snapshot and
   * after every series. With [refresh] each series listing bypasses its cache
   * TTL and the series snapshot itself is refetched from the server; without
   * it everything reads through caches, which lets an interrupted scan
   * fast-forward to where it left off.
   */
  fun scan(refresh: Boolean): Flow<Progress> = channelFlow {
    // Stale scans read the local snapshot so they never rewrite the series
    // cache (which would reset the series list screen's pagination); an
    // explicit scan also kicks off a listing refresh so new series land for
    // this or the next pass.
    val allSeries = seriesRepository.observeAllSeries(refresh = refresh).first()
      .sortedByDescending { it.updatedAt }

    var progress = Progress(done = 0, total = allSeries.size, skippedCount = 0, failedCount = 0)
    val mutex = Mutex()
    send(progress)

    try {
      val semaphore = Semaphore(SCAN_CONCURRENCY)
      coroutineScope {
        allSeries.forEach { series ->
          launch {
            semaphore.withPermit {
              val outcome = scanSeries(series, refresh)
              mutex.withLock {
                progress = progress.copy(
                  done = progress.done + 1,
                  skippedCount = progress.skippedCount + (if (outcome == Outcome.Skipped) 1 else 0),
                  failedCount = progress.failedCount + (if (outcome == Outcome.Failed) 1 else 0),
                )
                send(progress)
              }
            }
          }
        }
      }
    } catch (e: RateLimitStop) {
      send(progress.copy(rateLimited = true))
    }
  }

  private suspend fun scanSeries(series: Series, refresh: Boolean): Outcome {
    val owned = series.books.orEmpty()
    // Pre-check identifiability so unscannable series don't cost a fetch.
    if (seriesMatch(series.name, owned) == null) return Outcome.Skipped

    return when (val result = bookInfoRegistry.fetchSeriesEntries(series.name, owned, refresh)) {
      is SeriesFetchResult.Success -> Outcome.Scanned
      SeriesFetchResult.Unavailable -> Outcome.Skipped
      SeriesFetchResult.Error -> Outcome.Failed
      is SeriesFetchResult.RateLimited -> {
        delay(result.retryAfter ?: RATE_LIMIT_PAUSE)
        when (bookInfoRegistry.fetchSeriesEntries(series.name, owned, refresh)) {
          is SeriesFetchResult.Success -> Outcome.Scanned
          SeriesFetchResult.Unavailable -> Outcome.Skipped
          SeriesFetchResult.Error -> Outcome.Failed
          // Still limited after waiting it out — stop the whole scan and let
          // the caller resume from the cache checkpoint later.
          is SeriesFetchResult.RateLimited -> throw RateLimitStop()
        }
      }
    }
  }

  private enum class Outcome { Scanned, Skipped, Failed }

  private class RateLimitStop : Exception("Provider rate limited the scan")
}
