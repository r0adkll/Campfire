// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class PodcastEpisodeDownload(
  val id: String,
  val episodeDisplayTitle: String? = null,
  val url: String,
  val libraryItemId: String,
  val libraryId: String? = null,
  val isFinished: Boolean = false,
  val failed: Boolean = false,
  val appendRandomId: Boolean = false,
  val startedAt: Long? = null,
  val createdAt: Long? = null,
  val finishedAt: Long? = null,
  val podcastTitle: String? = null,
  val podcastExplicit: Boolean = false,
  val season: String? = null,
  val episode: String? = null,
  val episodeType: String = "full",
  val publishedAt: Long? = null,
  val guid: String? = null,
) : NetworkModel()
