// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import kotlin.time.Duration

interface PlaybackHistoryRecorder {

  fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration? = null,
    episodeId: PodcastEpisodeId? = null,
  )
}
