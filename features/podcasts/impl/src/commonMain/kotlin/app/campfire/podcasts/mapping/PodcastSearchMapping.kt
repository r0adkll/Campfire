package app.campfire.podcasts.mapping

import app.campfire.network.models.PodcastFeed
import app.campfire.network.models.PodcastMetadata
import app.campfire.network.models.PodcastSearchResultDto
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastSearchResult
import kotlin.time.Instant

/**
 * Map a search-endpoint hit into the domain model. Returns null when the result has no feed URL —
 * iTunes occasionally returns rows without one and they can't be added as podcast items.
 */
internal fun PodcastSearchResultDto.asDomainModelOrNull(): PodcastSearchResult? {
  val url = feedUrl?.takeIf { it.isNotBlank() } ?: return null
  return PodcastSearchResult(
    itunesId = id.toString(),
    itunesArtistId = artistId?.toString(),
    title = title,
    author = artistName?.takeIf { it.isNotBlank() },
    descriptionHtml = description?.takeIf { it.isNotBlank() },
    descriptionPlain = descriptionPlain?.takeIf { it.isNotBlank() },
    coverUrl = cover?.takeIf { it.isNotBlank() },
    feedUrl = url,
    itunesPageUrl = pageUrl?.takeIf { it.isNotBlank() },
    releaseDateIso = releaseDate?.takeIf { it.isNotBlank() },
    genres = genres,
    trackCount = trackCount,
    explicit = explicit,
  )
}

/**
 * Synthesize a draft from a parsed RSS feed. Returns null when the feed lacks a title — without
 * one there's nothing to display and nothing to send to the create endpoint. Falls back to
 * [fallbackFeedUrl] (the URL the user pasted) when the parsed feed doesn't advertise its own.
 */
internal fun PodcastFeed.asDraftOrNull(fallbackFeedUrl: String): PodcastDraft? {
  val meta = metadata ?: return null
  val title = meta.title?.takeIf { it.isNotBlank() } ?: return null
  return PodcastDraft(
    title = title,
    author = meta.author?.takeIf { it.isNotBlank() },
    descriptionHtml = meta.description?.takeIf { it.isNotBlank() },
    descriptionPlain = meta.descriptionPlain?.takeIf { it.isNotBlank() },
    coverUrl = meta.image?.takeIf { it.isNotBlank() },
    feedUrl = meta.feedUrl?.takeIf { it.isNotBlank() } ?: fallbackFeedUrl,
    itunesId = null,
    itunesArtistId = null,
    itunesPageUrl = null,
    releaseDateIso = meta.pubDate?.takeIf { it.isNotBlank() },
    language = meta.language?.takeIf { it.isNotBlank() },
    genres = meta.categories,
    explicit = meta.explicit.matchesExplicit(),
    episodeType = meta.type?.takeIf { it.isNotBlank() },
  )
}

private fun String?.matchesExplicit(): Boolean {
  if (this == null) return false
  return when (lowercase()) {
    "yes", "true", "explicit" -> true
    else -> false
  }
}

/**
 * Build the create-endpoint metadata payload from a draft. Tries to parse the ISO-8601 release
 * date; falls back to null when the value uses a feed-native format (e.g. RFC 2822 `pubDate`) the
 * server can't ingest as an [Instant].
 */
internal fun PodcastDraft.asCreateMetadata(): PodcastMetadata {
  return PodcastMetadata(
    title = title,
    author = author,
    description = descriptionPlain ?: descriptionHtml,
    releaseDate = releaseDateIso?.parseInstantOrNull(),
    genres = genres,
    feedUrl = feedUrl,
    imageUrl = coverUrl,
    itunesPageUrl = itunesPageUrl,
    itunesId = itunesId,
    itunesArtistId = itunesArtistId,
    explicit = explicit,
    language = language,
    type = episodeType,
  )
}

private fun String.parseInstantOrNull(): Instant? {
  return try {
    Instant.parse(this)
  } catch (_: Throwable) {
    null
  }
}
