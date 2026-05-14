package app.campfire.podcasts.mapping

import app.campfire.network.models.RssPodcastEpisode
import app.campfire.podcasts.api.RemotePodcastEpisode

internal fun RssPodcastEpisode.asDomainModel(): RemotePodcastEpisode {
  return RemotePodcastEpisode(
    title = title,
    subtitle = subtitle.takeIf { it.isNotBlank() },
    description = description.takeIf { it.isNotBlank() },
    descriptionPlain = descriptionPlain.takeIf { it.isNotBlank() },
    author = author.takeIf { it.isNotBlank() },
    episodeType = episodeType.takeIf { it.isNotBlank() },
    season = season.takeIf { it.isNotBlank() },
    episode = episode.takeIf { it.isNotBlank() },
    pubDate = pubDate.takeIf { it.isNotBlank() },
    publishedAtMillis = publishedAt,
    durationInSeconds = durationSeconds,
    explicit = explicit.equals("true", ignoreCase = true),
    enclosureUrl = enclosure.url,
    enclosureType = enclosure.type,
    enclosureLength = enclosure.length?.toLongOrNull(),
    guid = guid,
    chaptersUrl = chaptersUrl,
    chaptersType = chaptersType,
  )
}

internal fun RemotePodcastEpisode.asNetworkModel(): RssPodcastEpisode {
  return RssPodcastEpisode(
    title = title,
    subtitle = subtitle.orEmpty(),
    description = description.orEmpty(),
    descriptionPlain = descriptionPlain.orEmpty(),
    pubDate = pubDate.orEmpty(),
    episodeType = episodeType.orEmpty(),
    season = season.orEmpty(),
    episode = episode.orEmpty(),
    author = author.orEmpty(),
    duration = "",
    durationSeconds = durationInSeconds,
    explicit = if (explicit) "true" else "false",
    publishedAt = publishedAtMillis,
    enclosure = RssPodcastEpisode.Enclosure(
      url = enclosureUrl,
      type = enclosureType,
      length = enclosureLength?.toString(),
    ),
    guid = guid,
    chaptersUrl = chaptersUrl,
    chaptersType = chaptersType,
  )
}
