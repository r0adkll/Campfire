// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Envelope for `GET /api/libraries/:id/recent-episodes`. The server does not return a `total`
 * or `nextPage`; callers should compute end-of-pagination as `episodes.size < limit`.
 */
@Serializable
data class PagedRecentEpisodesResponse(
  val episodes: List<RecentPodcastEpisode>,
  val limit: Int,
  val page: Int,
)
