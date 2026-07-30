// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.db

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SessionDataSource {

  fun observeCurrentSession(): Flow<Session?>

  suspend fun getCurrentSession(): Session?

  suspend fun getSession(libraryItemId: LibraryItemId): Session?

  suspend fun getSessions(userId: UserId): List<Session>

  suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    progress: MediaProgress?,
    episodeId: PodcastEpisodeId? = null,
  ): Session

  suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  )

  suspend fun updateLastPlayed(
    libraryItemId: LibraryItemId,
  )

  suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  )

  suspend fun markDeleted(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  suspend fun deleteSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  suspend fun stopSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )
}
