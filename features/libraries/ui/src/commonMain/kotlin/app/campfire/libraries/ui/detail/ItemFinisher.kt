// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.history.recordFinished
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressRepository
import dev.zacsweers.metro.Inject

/** Marks an item finished, or not, as its detail screen and its card's actions both do. */
@Inject
class ItemFinisher(
  private val playbackController: PlaybackController,
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRecorder: PlaybackHistoryRecorder,
) {

  /**
   * Marks [item] finished, after ending its listening session when it [isPlaying], so playing
   * on doesn't take the finish back.
   */
  suspend fun markFinished(item: LibraryItem, isPlaying: Boolean) {
    if (isPlaying) playbackController.stopSession(item.id)
    sessionsRepository.markDeleted(item.id)
    mediaProgressRepository.markFinished(item.id)
    playbackHistoryRecorder.recordFinished(item.id, item.media.duration)
  }

  suspend fun markNotFinished(itemId: LibraryItemId) {
    mediaProgressRepository.markNotFinished(itemId)
  }
}
