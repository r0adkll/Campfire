package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode as DomainPodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.ShelfEntity
import app.campfire.data.PodcastEpisode as DbPodcastEpisode
import app.campfire.data.PodcastMedia as DbPodcastMedia
import app.campfire.data.mapping.model.EpisodeShelfRow
import app.campfire.data.mapping.model.PodcastLibraryItemWithMedia
import app.campfire.network.models.Podcast as NetworkPodcast
import app.campfire.network.models.PodcastEpisode as NetworkPodcastEpisode
import app.campfire.network.models.PodcastMetadata as NetworkPodcastMetadata
import kotlin.time.Duration.Companion.seconds

// ===================================================================================
// Network → Domain
// ===================================================================================

fun NetworkPodcastMetadata.asDomainModel(): Media.Metadata.Podcast {
  return Media.Metadata.Podcast(
    title = title,
    // The server occasionally sends a populated `titleIgnorePrefix`; the auto-generated
    // PodcastMetadata DTO doesn't model it, so we fall back to title here.
    titleIgnorePrefix = title,
    author = author,
    description = description,
    releaseDate = releaseDate?.toString(),
    genres = genres ?: emptyList(),
    feedUrl = feedUrl,
    imageUrl = imageUrl,
    itunesPageUrl = itunesPageUrl,
    itunesId = itunesId,
    // Server occasionally returns "" rather than omitting it; treat empty as missing.
    itunesArtistId = itunesArtistId?.takeIf { it.isNotEmpty() },
    isExplicit = explicit ?: false,
    language = language,
    podcastType = type,
  )
}

fun NetworkPodcastEpisode.asDomainModel(
  urlHydrator: UrlHydrator,
): DomainPodcastEpisode {
  // Episode duration may be missing on the basic JSON shape — fall back to the
  // underlying audioFile's duration which is always present.
  val durationMillis = duration?.seconds?.inWholeMilliseconds
    ?: audioFile.duration.toDouble().seconds.inWholeMilliseconds

  // Size is only present on the expanded shape; fall back to audioFile.metadata.size.
  val sizeBytes = propertySize?.toLong() ?: audioFile.metadata.size

  return DomainPodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastId = podcastId,
    index = index,
    season = season?.takeIf { it.isNotEmpty() },
    episode = episode?.takeIf { it.isNotEmpty() },
    episodeType = episodeType?.takeIf { it.isNotEmpty() },
    title = title,
    subtitle = subtitle?.takeIf { it.isNotEmpty() },
    description = description?.takeIf { it.isNotEmpty() },
    pubDate = pubDate?.takeIf { it.isNotEmpty() },
    publishedAtMillis = publishedAt,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    durationInMillis = durationMillis,
    sizeInBytes = sizeBytes,
    audioTrack = audioTrack?.asDomainModel(urlHydrator),
    chapters = emptyList(),
  )
}

fun NetworkPodcast.asDomainModel(
  libraryItemId: LibraryItemId,
  urlHydrator: UrlHydrator,
): Media.Podcast {
  val episodeList = episodes
    ?.map { it.asDomainModel(urlHydrator) }
    .orEmpty()

  val metadata = metadata?.asDomainModel() ?: Media.Metadata.Podcast(
    title = null,
    titleIgnorePrefix = null,
    author = null,
    description = null,
    releaseDate = null,
    genres = emptyList(),
    feedUrl = null,
    imageUrl = null,
    itunesPageUrl = null,
    itunesId = null,
    itunesArtistId = null,
    isExplicit = false,
    language = null,
    podcastType = null,
  )

  return Media.Podcast(
    id = id,
    metadata = metadata,
    coverImageUrl = urlHydrator.hydrateLibraryItem(libraryItemId),
    coverPath = coverPath,
    tags = tags ?: emptyList(),
    sizeInBytes = propertySize?.toLong() ?: episodeList.sumOf { it.sizeInBytes },
    episodes = episodeList,
    numEpisodes = numTracks ?: episodeList.size,
    autoDownloadEpisodes = autoDownloadEpisodes ?: false,
    autoDownloadSchedule = autoDownloadSchedule,
    lastEpisodeCheckMillis = lastEpisodeCheck,
    maxEpisodesToKeep = maxEpisodesToKeep ?: 0,
    maxNewEpisodesToDownload = maxNewEpisodesToDownload ?: 3,
    latestEpisodePublishedAtMillis = latestEpisodePublished?.toLong(),
  )
}

// ===================================================================================
// Network → DB
// ===================================================================================

/**
 * Map the network [NetworkPodcast] to a [DbPodcastMedia] row. Routed through the domain
 * model first so the same field-derivation logic (size fallback, metadata defaults) is
 * applied consistently across read and write paths.
 */
fun NetworkPodcast.asDbModel(
  libraryItemId: LibraryItemId,
  urlHydrator: UrlHydrator,
): DbPodcastMedia = asDomainModel(libraryItemId, urlHydrator)
  .asDbModel(libraryItemId)

fun NetworkPodcastEpisode.asDbModel(
  libraryItemId: LibraryItemId,
  podcastMediaId: String,
): DbPodcastEpisode {
  val durationMillis = duration?.seconds?.inWholeMilliseconds
    ?: audioFile.duration.toDouble().seconds.inWholeMilliseconds
  val sizeBytes = propertySize?.toLong() ?: audioFile.metadata.size

  return DbPodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastMediaId = podcastMediaId,
    episodeIndex = index,
    season = season?.takeIf { it.isNotEmpty() },
    episodeNumber = episode?.takeIf { it.isNotEmpty() },
    episodeType = episodeType?.takeIf { it.isNotEmpty() },
    title = title,
    subtitle = subtitle?.takeIf { it.isNotEmpty() },
    description = description?.takeIf { it.isNotEmpty() },
    pubDate = pubDate?.takeIf { it.isNotEmpty() },
    publishedAtMillis = publishedAt,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    durationInMillis = durationMillis,
    sizeInBytes = sizeBytes,
  )
}

// ===================================================================================
// Domain → DB
// ===================================================================================

fun Media.Podcast.asDbModel(libraryItemId: LibraryItemId): DbPodcastMedia {
  return DbPodcastMedia(
    mediaId = id,
    libraryItemId = libraryItemId,
    coverPath = coverPath,
    tags = tags,
    sizeInBytes = sizeInBytes,
    numEpisodes = numEpisodes,
    autoDownloadEpisodes = autoDownloadEpisodes,
    autoDownloadSchedule = autoDownloadSchedule,
    lastEpisodeCheckMillis = lastEpisodeCheckMillis,
    maxEpisodesToKeep = maxEpisodesToKeep,
    maxNewEpisodesToDownload = maxNewEpisodesToDownload,
    latestEpisodePublishedAtMillis = latestEpisodePublishedAtMillis,

    metadata_title = metadata.title,
    metadata_titleIgnorePrefix = metadata.titleIgnorePrefix,
    metadata_author = metadata.author,
    metadata_description = metadata.description,
    metadata_releaseDate = metadata.releaseDate,
    metadata_genres = metadata.genres,
    metadata_feedUrl = metadata.feedUrl,
    metadata_imageUrl = metadata.imageUrl,
    metadata_itunesPageUrl = metadata.itunesPageUrl,
    metadata_itunesId = metadata.itunesId,
    metadata_itunesArtistId = metadata.itunesArtistId,
    metadata_explicit = metadata.isExplicit,
    metadata_language = metadata.language,
    metadata_podcastType = metadata.podcastType,
  )
}

fun DomainPodcastEpisode.asDbModel(podcastMediaId: String): DbPodcastEpisode {
  return DbPodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastMediaId = podcastMediaId,
    episodeIndex = index,
    season = season,
    episodeNumber = episode,
    episodeType = episodeType,
    title = title,
    subtitle = subtitle,
    description = description,
    pubDate = pubDate,
    publishedAtMillis = publishedAtMillis,
    addedAtMillis = addedAtMillis,
    updatedAtMillis = updatedAtMillis,
    durationInMillis = durationInMillis,
    sizeInBytes = sizeInBytes,
  )
}

// ===================================================================================
// DB → Domain
// ===================================================================================

fun DbPodcastEpisode.asDomainModel(
  audioTrack: AudioTrack? = null,
): DomainPodcastEpisode {
  return DomainPodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastId = podcastMediaId,
    index = episodeIndex,
    season = season,
    episode = episodeNumber,
    episodeType = episodeType,
    title = title,
    subtitle = subtitle,
    description = description,
    pubDate = pubDate,
    publishedAtMillis = publishedAtMillis,
    addedAtMillis = addedAtMillis,
    updatedAtMillis = updatedAtMillis,
    durationInMillis = durationInMillis,
    sizeInBytes = sizeInBytes,
    audioTrack = audioTrack,
    chapters = emptyList(),
  )
}

/**
 * Compose a domain [LibraryItem] from the joined podcastMedia row plus its episodes.
 * Mirrors [LibraryItemWithMedia.asDomainModel] for the book path.
 */
fun PodcastLibraryItemWithMedia.asDomainModel(
  urlHydrator: UrlHydrator,
  episodes: List<DbPodcastEpisode>,
  audioTracksByEpisodeId: Map<PodcastEpisodeId, AudioTrack> = emptyMap(),
): LibraryItem {
  val metadata = Media.Metadata.Podcast(
    title = metadata_title,
    titleIgnorePrefix = metadata_titleIgnorePrefix,
    author = metadata_author,
    description = metadata_description,
    releaseDate = metadata_releaseDate,
    genres = metadata_genres ?: emptyList(),
    feedUrl = metadata_feedUrl,
    imageUrl = metadata_imageUrl,
    itunesPageUrl = metadata_itunesPageUrl,
    itunesId = metadata_itunesId,
    itunesArtistId = metadata_itunesArtistId,
    isExplicit = metadata_explicit,
    language = metadata_language,
    podcastType = metadata_podcastType,
  )

  val podcast = Media.Podcast(
    id = mediaId,
    metadata = metadata,
    coverImageUrl = urlHydrator.hydrateLibraryItem(id),
    coverPath = coverPath,
    tags = tags ?: emptyList(),
    sizeInBytes = sizeInBytes,
    episodes = episodes.map { it.asDomainModel(audioTracksByEpisodeId[it.id]) },
    numEpisodes = numEpisodes,
    autoDownloadEpisodes = autoDownloadEpisodes,
    autoDownloadSchedule = autoDownloadSchedule,
    lastEpisodeCheckMillis = lastEpisodeCheckMillis,
    maxEpisodesToKeep = maxEpisodesToKeep,
    maxNewEpisodesToDownload = maxNewEpisodesToDownload,
    latestEpisodePublishedAtMillis = latestEpisodePublishedAtMillis,
  )

  return LibraryItem(
    id = id,
    ino = ino,
    libraryId = libraryId,
    oldLibraryId = oldLibraryItemId,
    folderId = folderId,
    path = path,
    relPath = relPath,
    isFile = isFile,
    mtimeMs = mtimeMs,
    ctimeMs = ctimeMs,
    birthtimeMs = birthtimeMs,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = MediaType.Podcast,
    numFiles = numFiles,
    sizeInBytes = size,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = podcast,
    userMediaProgress = userMediaProgress?.asDomainModel(),
  )
}

/**
 * Compose the home-feed [ShelfEntity.EpisodeShelfEntry] from a joined libraryItem +
 * podcastMedia + podcastEpisode shelf row. The pairing tells the UI both which podcast
 * the entry represents and which episode the shelf is highlighting.
 */
fun EpisodeShelfRow.asDomainModel(
  urlHydrator: UrlHydrator,
): ShelfEntity.EpisodeShelfEntry {
  return ShelfEntity.EpisodeShelfEntry(
    libraryItem = item.asDomainModel(urlHydrator, episodes = emptyList()),
    recentEpisode = episode.asDomainModel(),
  )
}
