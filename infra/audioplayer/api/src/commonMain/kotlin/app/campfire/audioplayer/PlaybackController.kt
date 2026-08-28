// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId

/**
 * This interface is the means by which the app can a certain the [AudioPlayer] for any ongoing playback, or not
 * if none currently. Then also start a new playback session
 */
interface PlaybackController {

  /**
   * Start a new playback session for a given library item.
   *
   * @param methodOverride Optional per-listen delivery override — forces HLS
   * ([app.campfire.core.model.PlayMethod.Transcode]) or direct play
   * ([app.campfire.core.model.PlayMethod.DirectPlay]) for this session only, winning over
   * the streaming-method setting.
   */
  fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
    episodeId: PodcastEpisodeId? = null,
    methodOverride: PlayMethod? = null,
  )

  /**
   * Stop a current session. Pass [episodeId] to scope the stop to a podcast episode session
   * — books and other episodes are unaffected.
   */
  fun stopSession(
    itemId: LibraryItemId,
    clearQueue: Boolean = false,
    episodeId: PodcastEpisodeId? = null,
  )
}
