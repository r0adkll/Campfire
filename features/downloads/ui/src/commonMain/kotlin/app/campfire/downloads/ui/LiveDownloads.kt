// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.offline.Download
import app.campfire.offline.Downloads
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Every download, with those still to finish kept up to date as they go. [Downloads.observeAll]
 * alone may only report progress when a download changes state, which leaves a row's progress
 * standing still, so the downloads in progress are also followed with [Downloads.observe].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Downloads.observeAllWithProgress(): Flow<List<Download>> {
  val all = observeAll()
  val inProgress = all
    .map { downloads -> downloads.filter { it.isInProgress }.map { it.content }.toSet() }
    .distinctUntilChanged()
    .flatMapLatest { contents ->
      // Starts empty so the list never waits on a download's first progress report
      if (contents.isEmpty()) flowOf(emptyMap()) else observe(contents).onStart { emit(emptyMap()) }
    }
  return combine(all, inProgress) { downloads, live ->
    downloads.map { download -> if (download.isInProgress) live[download.content] ?: download else download }
  }.distinctUntilChanged()
}
