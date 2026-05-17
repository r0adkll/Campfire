package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Podcast-level metadata as returned by `POST /api/podcasts/feed`. This is distinct from the
 * strictly-typed [PodcastMetadata] (which models the server's stored representation): the feed
 * response uses RSS-native shapes — notably [explicit] is a string (`"clean"`, `"yes"`, `"no"`),
 * the artwork field is named [image] (not `imageUrl`), and [categories] replaces `genres`.
 *
 * Used by the "Add podcast" flow to preview a feed before creating the library item.
 */
@Serializable
data class PodcastFeedMetadata(
  val title: String? = null,
  val author: String? = null,
  val description: String? = null,
  val descriptionPlain: String? = null,
  val image: String? = null,
  val feedUrl: String? = null,
  val language: String? = null,
  val explicit: String? = null,
  val pubDate: String? = null,
  val link: String? = null,
  val categories: List<String> = emptyList(),
  val type: String? = null,
)
