package app.campfire.podcasts.api

/**
 * One result from a podcast search. Results without a feed URL are filtered out at the repository
 * boundary, so [feedUrl] is non-null here — without it there is no way to create a podcast item.
 */
data class PodcastSearchResult(
  val itunesId: String,
  val itunesArtistId: String?,
  val title: String,
  val author: String?,
  val descriptionHtml: String?,
  val descriptionPlain: String?,
  val coverUrl: String?,
  val feedUrl: String,
  val itunesPageUrl: String?,
  val releaseDateIso: String?,
  val genres: List<String>,
  val trackCount: Int?,
  val explicit: Boolean,
)

fun PodcastSearchResult.toDraft(): PodcastDraft = PodcastDraft(
  title = title,
  author = author,
  descriptionHtml = descriptionHtml,
  descriptionPlain = descriptionPlain,
  coverUrl = coverUrl,
  feedUrl = feedUrl,
  itunesId = itunesId,
  itunesArtistId = itunesArtistId,
  itunesPageUrl = itunesPageUrl,
  releaseDateIso = releaseDateIso,
  language = null,
  genres = genres,
  explicit = explicit,
)

/**
 * Merge another draft (typically the feed-derived one) into this one, filling holes only — the
 * receiver's non-null/non-empty fields take precedence. Mirrors how the Audiobookshelf web client
 * populates the "Add podcast" form: iTunes Search returns `""` for `description` on almost every
 * result, so the feed-derived draft fills that in while the iTunes fields (cover, itunesId, etc.)
 * stay untouched.
 */
fun PodcastDraft.mergedWith(other: PodcastDraft): PodcastDraft {
  return copy(
    descriptionHtml = descriptionHtml ?: other.descriptionHtml,
    descriptionPlain = descriptionPlain ?: other.descriptionPlain,
    coverUrl = coverUrl ?: other.coverUrl,
    language = language ?: other.language,
    genres = genres.ifEmpty { other.genres },
    episodeType = episodeType ?: other.episodeType,
    releaseDateIso = releaseDateIso ?: other.releaseDateIso,
    explicit = explicit || other.explicit,
    author = author ?: other.author,
  )
}
