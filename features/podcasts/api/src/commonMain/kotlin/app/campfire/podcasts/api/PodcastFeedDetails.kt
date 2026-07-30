// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api

/**
 * Full parsed feed result: the podcast-level [draft] (title, author, description, image, etc.)
 * plus every [episodes] entry the feed advertises. Returned by
 * [PodcastsRepository.fetchPodcastFeedDetails] — callers consume one or both depending on context
 * (FindEpisodes uses just [episodes], the Add Podcast builder uses both).
 */
data class PodcastFeedDetails(
  val draft: PodcastDraft,
  val episodes: List<RemotePodcastEpisode>,
)
