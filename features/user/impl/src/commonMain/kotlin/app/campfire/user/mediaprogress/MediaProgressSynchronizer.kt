// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.MediaProgress
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asNetworkUpdate
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.measureTime
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface MediaProgressSynchronizer {

  suspend fun sync(mediaProgress: MediaProgress, force: Boolean = false)
}

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultMediaProgressSynchronizer(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressSynchronizer {

  private val dispatcher = dispatcherProvider.io.limitedParallelism(1, "MediaProgressSync")

  // Keyed per (libraryItemId, episodeId?) so per-episode syncs don't share a throttle
  // timestamp with sibling episodes or with the parent book row.
  private var lastSyncTimes = mutableMapOf<SyncKey, Long>()

  override suspend fun sync(mediaProgress: MediaProgress, force: Boolean) = withContext(dispatcher) {
    val key = SyncKey(mediaProgress.libraryItemId, mediaProgress.episodeId)
    val lastSync = lastSyncTimes[key] ?: 0
    val elapsed = fatherTime.nowInEpochMillis() - lastSync

    if (force || mediaProgress.id == MediaProgress.UNKNOWN_ID || elapsed > MIN_SYNC_TIME) {
      syncInternal(mediaProgress)
    }
  }

  private suspend fun syncInternal(mediaProgress: MediaProgress) = measureTime {
    val key = SyncKey(mediaProgress.libraryItemId, mediaProgress.episodeId)
    val result = api.updateMediaProgress(
      libraryItemId = mediaProgress.libraryItemId,
      episodeId = mediaProgress.episodeId,
      update = mediaProgress.asNetworkUpdate(),
    )

    if (result.isSuccess) {
      if (mediaProgress.id == MediaProgress.UNKNOWN_ID) {
        // If we just synced a new media progress item to the server we need to re-pull it so that we have an updated id
        val updatedMediaProgress = api.getMediaProgress(
          libraryItemId = mediaProgress.libraryItemId,
          episodeId = mediaProgress.episodeId,
        ).getOrNull()
        if (updatedMediaProgress != null) {
          // NOTE: this insert bypasses MediaProgressSourceOfTruthFactory.insertIfFresher
          // because we need the just-issued server id. If the server's lastUpdate here
          // is older than the local row a concurrent tick already wrote, this path can
          // clobber the fresher local row.
          withContext(dispatcherProvider.databaseWrite) {
            db.mediaProgressQueries.insert(
              updatedMediaProgress.asDbModel(),
            )
          }
        }
        lastSyncTimes[key] = fatherTime.nowInEpochMillis()
      } else {
        lastSyncTimes[key] = fatherTime.nowInEpochMillis()
      }
    }
  }

  private data class SyncKey(
    val libraryItemId: String,
    val episodeId: String? = null,
  )
}

private const val MIN_SYNC_TIME = 15_000L // 15 seconds
