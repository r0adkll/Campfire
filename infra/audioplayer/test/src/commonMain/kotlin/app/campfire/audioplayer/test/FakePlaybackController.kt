// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId

class FakePlaybackController : PlaybackController {

  var session: PlaybackControllerSession = PlaybackControllerSession.None

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
    episodeId: PodcastEpisodeId?,
  ) {
    session = PlaybackControllerSession.Started(
      itemId = itemId,
      playImmediately = playImmediately,
      chapterId = chapterId,
      episodeId = episodeId,
    )
  }

  override fun stopSession(
    itemId: LibraryItemId,
    clearQueue: Boolean,
    episodeId: PodcastEpisodeId?,
  ) {
    session = PlaybackControllerSession.Stopped(
      itemId = itemId,
      clearQueue = clearQueue,
      episodeId = episodeId,
    )
  }
}

sealed interface PlaybackControllerSession {
  data object None : PlaybackControllerSession

  data class Started(
    val itemId: LibraryItemId,
    val playImmediately: Boolean,
    val chapterId: Int?,
    val episodeId: PodcastEpisodeId?,
  ) : PlaybackControllerSession

  data class Stopped(
    val itemId: LibraryItemId,
    val clearQueue: Boolean,
    val episodeId: PodcastEpisodeId? = null,
  ) : PlaybackControllerSession
}
