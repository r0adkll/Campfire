// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test.history

import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import kotlin.time.Duration

class FakePlaybackHistoryRecorder : PlaybackHistoryRecorder {

  val records = mutableListOf<Record>()

  override fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration?,
    episodeId: PodcastEpisodeId?,
  ) {
    records += Record(libraryItemId, type, fromPosition, toPosition, episodeId)
  }

  data class Record(
    val libraryItemId: LibraryItemId,
    val type: PlaybackActionType,
    val fromPosition: Duration,
    val toPosition: Duration? = null,
    val episodeId: PodcastEpisodeId? = null,
  )
}
