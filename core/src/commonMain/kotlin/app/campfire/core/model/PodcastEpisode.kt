// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.extensions.asSeconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias PodcastEpisodeId = String

/**
 * A single playable episode of a podcast.
 *
 * `index` is frequently null on the server — order episodes by [publishedAtMillis]
 * descending for display. `episode` is the RSS episode number as a string, not the
 * episode title. `description` is HTML-formatted.
 */
data class PodcastEpisode(
  val id: PodcastEpisodeId,
  val libraryItemId: LibraryItemId,
  val podcastId: MediaId,
  val index: Int? = null,
  val season: String? = null,
  val episode: String? = null,
  val episodeType: String? = null,
  val title: String,
  val subtitle: String? = null,
  val description: String? = null,
  val pubDate: String? = null,
  val publishedAtMillis: Long? = null,
  val addedAtMillis: Long,
  val updatedAtMillis: Long,
  val durationInMillis: Long,
  val sizeInBytes: Long,
  val audioTrack: AudioTrack? = null,
  val chapters: List<Chapter> = emptyList(),
) {
  val durationInSeconds: Float
    get() = durationInMillis.milliseconds.asSeconds()

  val duration: Duration
    get() = durationInMillis.milliseconds
}
