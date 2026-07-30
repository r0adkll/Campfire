// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A podcast containing multiple episodes.
 *
 * @param id The ID of podcasts and podcast episodes after 2.3.0.
 * @param libraryItemId The ID of library items after 2.3.0.
 * @param metadata
 * @param coverPath The file path to the podcast's cover image.
 * @param tags The tags associated with the podcast.
 * @param episodes The episodes of the podcast.
 * @param autoDownloadEpisodes Whether episodes are automatically downloaded.
 * @param autoDownloadSchedule The schedule for automatic episode downloads, in cron format.
 * @param lastEpisodeCheck The timestamp of the last episode check.
 * @param maxEpisodesToKeep The maximum number of episodes to keep.
 * @param maxNewEpisodesToDownload The maximum number of new episodes to download when automatically downloading epsiodes.
 * @param lastCoverSearch The timestamp of the last cover search.
 * @param lastCoverSearchQuery The query used for the last cover search.
 * @param propertySize The total size of all episodes in bytes.
 * @param duration The total duration of all episodes in seconds.
 * @param numTracks The number of tracks (episodes) in the podcast.
 * @param latestEpisodePublished The timestamp of the most recently published episode.
 */
@Serializable
data class Podcast(
  /* The ID of podcasts and podcast episodes after 2.3.0. */
  val id: String,

  // This is apparently only set on the expanded item
  val libraryItemId: String? = null,

  val metadata: PodcastMetadata? = null,

  /* The file path to the podcast's cover image. */
  val coverPath: String? = null,

  /* The tags associated with the podcast. */
  val tags: List<String>? = null,

  /* The episodes of the podcast. */
  val episodes: List<PodcastEpisode>? = null,

  /* Whether episodes are automatically downloaded. */
  val autoDownloadEpisodes: Boolean? = null,

  /* The schedule for automatic episode downloads, in cron format. */
  val autoDownloadSchedule: String? = null,

  /* The timestamp of the last episode check. */
  val lastEpisodeCheck: Long? = null,

  /* The maximum number of episodes to keep. */
  val maxEpisodesToKeep: Int? = null,

  /* The maximum number of new episodes to download when automatically downloading epsiodes. */
  val maxNewEpisodesToDownload: Int? = null,

  /* The timestamp of the last cover search. */
  val lastCoverSearch: Long? = null,

  /* The query used for the last cover search. */
  val lastCoverSearchQuery: String? = null,

  /* The total size of all episodes in bytes. */
  @SerialName(value = "size")
  val propertySize: Long? = null,

  /* The total duration of all episodes in seconds. */
  val duration: Int? = null,

  /* The number of tracks (episodes) in the podcast. */
  val numTracks: Int? = null,

  /* The timestamp of the most recently published episode. */
  val latestEpisodePublished: Long? = null,
)
