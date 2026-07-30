// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An episode discovered in a podcast's RSS feed but not (necessarily) yet downloaded to the server.
 * Returned by [PodcastsRepository.searchEpisodes] and round-tripped to
 * [PodcastsRepository.queueEpisodeDownloads] to enqueue a server-side download.
 *
 * The [enclosureUrl] is the dedup key both client-side (against already-downloaded episodes) and
 * server-side (the server silently rejects duplicate queue entries by URL).
 */
data class RemotePodcastEpisode(
  val title: String,
  val subtitle: String? = null,
  val description: String? = null,
  val descriptionPlain: String? = null,
  val author: String? = null,
  val episodeType: String? = null,
  val season: String? = null,
  val episode: String? = null,
  val pubDate: String? = null,
  val publishedAtMillis: Long? = null,
  val durationInSeconds: Int? = null,
  val explicit: Boolean = false,
  val enclosureUrl: String,
  val enclosureType: String? = null,
  val enclosureLength: Long? = null,
  val guid: String? = null,
  val chaptersUrl: String? = null,
  val chaptersType: String? = null,
) {
  val duration: Duration?
    get() = durationInSeconds?.seconds
}
