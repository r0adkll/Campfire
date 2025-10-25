package app.campfire.user.mediaprogress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.loggableId
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

  private var lastSyncTimes = mutableMapOf<String, Long>()

  override suspend fun sync(mediaProgress: MediaProgress, force: Boolean) = withContext(dispatcher) {
    val lastSync = lastSyncTimes[mediaProgress.libraryItemId] ?: 0
    val elapsed = fatherTime.nowInEpochMillis() - lastSync

    if (force || mediaProgress.id == MediaProgress.UNKNOWN_ID || elapsed > MIN_SYNC_TIME) {
      val duration = syncInternal(mediaProgress)
      bark(LogPriority.INFO) {
        "Syncing MediaProgress(${mediaProgress.libraryItemId.loggableId}) took $duration"
      }
    }
  }

  private suspend fun syncInternal(mediaProgress: MediaProgress) = measureTime {
    // push to the network
    val result = api.updateMediaProgress(
      libraryItemId = mediaProgress.libraryItemId,
      update = mediaProgress.asNetworkUpdate(),
    )

    if (result.isSuccess) {
      if (mediaProgress.id == MediaProgress.UNKNOWN_ID) {
        // If we just synced a new media progress item to the server we need to re-pull it so that we have an updated id
        val updatedMediaProgress = api.getMediaProgress(mediaProgress.libraryItemId).getOrNull()
        if (updatedMediaProgress != null) {
          // Now persist this to the database
          withContext(dispatcherProvider.databaseWrite) {
            db.mediaProgressQueries.insert(
              updatedMediaProgress.asDbModel(),
            )
          }

          bark(LogPriority.DEBUG) {
            "New MediaProgress Id ${updatedMediaProgress.libraryItemId.loggableId} " +
              "--> ${updatedMediaProgress.id}"
          }
          lastSyncTimes[mediaProgress.libraryItemId] = fatherTime.nowInEpochMillis()
        } else {
          lastSyncTimes[mediaProgress.libraryItemId] = fatherTime.nowInEpochMillis()
        }
      } else {
        lastSyncTimes[mediaProgress.libraryItemId] = fatherTime.nowInEpochMillis()
      }
    }
  }
}

private const val MIN_SYNC_TIME = 15_000L // 15 seconds
