// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.sessions.api.StreamingRoutePredictor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStreamingRoutePredictor : StreamingRoutePredictor {

  val wouldStreamHlsFlow = MutableStateFlow(false)

  var canStreamHls = false
  override fun canStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Boolean {
    return canStreamHls
  }

  override fun wouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Boolean {
    return wouldStreamHlsFlow.value
  }

  override fun observeWouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Flow<Boolean> {
    return wouldStreamHlsFlow
  }
}
