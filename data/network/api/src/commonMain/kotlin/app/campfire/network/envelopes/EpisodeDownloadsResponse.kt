// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.PodcastEpisodeDownload
import kotlinx.serialization.Serializable

/**
 * Wire-level response for `GET /api/libraries/{libraryId}/episode-downloads`.
 */
@Serializable
data class EpisodeDownloadsResponse(
  val currentDownload: PodcastEpisodeDownload? = null,
  val queue: List<PodcastEpisodeDownload> = emptyList(),
)
