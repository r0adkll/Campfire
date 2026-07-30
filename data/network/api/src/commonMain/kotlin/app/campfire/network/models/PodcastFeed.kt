// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The parsed feed returned by `POST /api/podcasts/feed`: podcast-level [metadata] plus the
 * [episodes] list. Used by both the "Find episodes" flow (which consumes only [episodes]) and
 * the "Add podcast" flow (which previews [metadata] before creating the library item).
 */
@Serializable
data class PodcastFeed(
  val metadata: PodcastFeedMetadata? = null,
  val episodes: List<RssPodcastEpisode> = emptyList(),
  val numEpisodes: Int = 0,
)
