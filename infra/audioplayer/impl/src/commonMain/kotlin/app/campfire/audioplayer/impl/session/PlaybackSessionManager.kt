// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.session

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId

/**
 * This interface is for connecting a live [app.campfire.audioplayer.AudioPlayer] instance
 * to a [app.campfire.core.model.Session] and keep it updated as the device playback is live.
 */
interface PlaybackSessionManager {

  /**
   * @param requeueCurrent when true and a different book or podcast episode is in the player,
   * it moves to the front of the queue instead of being dropped, unless the user turned
   * [app.campfire.settings.api.PlaybackSettings.requeueOnSwitch] off.
   */
  suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
    episodeId: PodcastEpisodeId? = null,
    methodOverride: PlayMethod? = null,
    requeueCurrent: Boolean = true,
  )

  suspend fun stopSession(
    libraryItemId: LibraryItemId,
    clearQueue: Boolean,
    episodeId: PodcastEpisodeId? = null,
  )
}
