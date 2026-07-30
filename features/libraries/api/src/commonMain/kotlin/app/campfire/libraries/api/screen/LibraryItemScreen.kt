// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class LibraryItemScreen(
  val libraryItemId: LibraryItemId,
  val sharedTransitionKey: String = libraryItemId,
  val episodeId: PodcastEpisodeId? = null,
) : DetailScreen(name = "LibraryItem")
