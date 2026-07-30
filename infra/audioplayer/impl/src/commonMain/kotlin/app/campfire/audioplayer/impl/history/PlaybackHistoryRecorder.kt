// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.history

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class PlaybackHistoryRecorderImpl(
  private val userSessionManager: UserSessionManager,
  private val database: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val playbackSettings: PlaybackSettings,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val coroutineScope: CoroutineScope,
) : PlaybackHistoryRecorder {

  override fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration?,
    episodeId: PodcastEpisodeId?,
  ) {
    coroutineScope.launch {
      if (!playbackSettings.playbackHistoryEnabled) return@launch
      val userId = userSessionManager.current.userId ?: return@launch
      write {
        database.playbackActionQueries.insert(
          libraryItemId = libraryItemId,
          userId = userId,
          // Empty string is the DB sentinel for "no episode" (book progress).
          episodeId = episodeId.orEmpty(),
          type = type,
          timestamp = fatherTime.now(),
          fromPosition = fromPosition,
          toPosition = toPosition,
        )
      }
    }
  }

  private suspend fun <T> write(block: suspend CoroutineScope.() -> T) = withContext(
    context = dispatcherProvider.databaseWrite,
    block = block,
  )
}
