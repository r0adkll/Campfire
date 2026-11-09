// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
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
  private val playbackHistoryRepository: PlaybackHistoryRepository,
) {

  /**
   * Marks [itemId] finished, after ending its listening session when it [isPlaying], so playing
   * on doesn't take the finish back.
   */
  suspend fun markFinished(itemId: LibraryItemId, isPlaying: Boolean) {
    if (isPlaying) playbackController.stopSession(itemId)
    sessionsRepository.markDeleted(itemId)
    mediaProgressRepository.markFinished(itemId)
    playbackHistoryRepository.clear(itemId)
  }

  suspend fun markNotFinished(itemId: LibraryItemId) {
    mediaProgressRepository.markNotFinished(itemId)
  }
}
