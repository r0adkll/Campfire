// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.reader.api.EbookRepository
import app.campfire.reader.api.asOfflineDownload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The ebooks kept to read offline, or on their way, as the download UI shows any other download.
 * They're kept by the reader rather than the offline download manager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun EbookRepository.observeOfflineDownloads(): Flow<List<OfflineDownload>> = observeDownloads()
  .flatMapLatest { itemIds ->
    if (itemIds.isEmpty()) {
      flowOf(emptyList())
    } else {
      combine(
        itemIds.map { itemId -> observeDownload(itemId).map { it.asOfflineDownload(itemId) } },
      ) { it.toList() }
    }
  }
