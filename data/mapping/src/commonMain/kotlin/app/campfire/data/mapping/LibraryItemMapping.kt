package app.campfire.data.mapping

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.AudioFile
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media as DomainMedia
import app.campfire.core.model.MediaType as DomainMediaType
import app.campfire.core.model.SeriesSequence
import app.campfire.core.util.createIfNotNull
import app.campfire.data.LibraryItem as DatabaseLibraryItem
import app.campfire.data.Media as DatabaseMedia
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MetadataAuthor
import app.campfire.data.SelectForAuthorName
import app.campfire.data.SelectForCollection
import app.campfire.data.SelectForSeries
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.network.RequestOrigin
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.LibraryItemBase
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.Media
import app.campfire.network.models.MediaExpanded
import app.campfire.network.models.MediaMinified as NetworkMediaMinified
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.network.models.MinifiedBookMetadata
import kotlin.time.Duration.Companion.seconds

fun LibraryItemBase.asDbModel(
  serverUrl: String? = null,
): DatabaseLibraryItem {
  return DatabaseLibraryItem(
    id = id,
    ino = ino,
    libraryId = libraryId,
    oldLibraryItemId = oldLibraryItemId,
    folderId = folderId,
    path = path,
    relPath = relPath,
    isFile = isFile,
    mtimeMs = mtimeMs,
    ctimeMs = ctimeMs,
    birthtimeMs = birthtimeMs,
    addedAt = addedAt,
    updatedAt = updatedAt,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = when (mediaType) {
      NetworkMediaType.Book -> DomainMediaType.Book
      NetworkMediaType.Podcast -> DomainMediaType.Podcast
      NetworkMediaType.Podcast2 -> DomainMediaType.Podcast
    },
    numFiles = numFiles ?: -1,
    size = size ?: -1,
    serverUrl = serverUrl ?: (origin as RequestOrigin.Url).serverUrl,
  )
}

fun LibraryItem.asDbModel(
  serverUrl: String,
): DatabaseLibraryItem {
  return DatabaseLibraryItem(
    id = id,
    ino = ino,
    libraryId = libraryId,
    oldLibraryItemId = oldLibraryId,
    folderId = folderId,
    path = path,
    relPath = relPath,
    isFile = isFile,
    mtimeMs = mtimeMs,
    ctimeMs = ctimeMs,
    birthtimeMs = birthtimeMs,
    addedAt = addedAtMillis,
    updatedAt = updatedAtMillis,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = mediaType,
    numFiles = numFiles,
    size = sizeInBytes,
    serverUrl = serverUrl,
  )
}

/*
 * LibraryItems will commonly come down with a 'seriesName' value
 * of "Some Book Series #5". We should be able to reasonably determine the
 * sequence # for ordering from this, or fallback onto another value if available.
 */
private val SERIES_SEQUENCE_REGEX = ".*#([0-9]+)".toRegex()

fun <T : Media> T.asDbModel(
  libraryItemId: String,
  fallbackSeriesSequence: Int? = null,
): DatabaseMedia {
  val metadata = when (this) {
    is NetworkMediaMinified<*> -> metadata
    is MediaExpanded -> metadata
    else -> error("Unknown media metadata")
  }

  val metadataAuthorName = metadata.authorName
    ?: (metadata as? ExpandedBookMetadata)?.authors?.firstOrNull()?.name

  val metadataAuthorNameLF = metadata.authorNameLF
    ?: (metadata as? ExpandedBookMetadata)?.authors?.firstOrNull()?.name?.lastFirst

  val metadataSeries = (metadata as? MinifiedBookMetadata)?.series
    ?: (metadata as? ExpandedBookMetadata)?.series?.firstOrNull()

  val metadataSeriesSequence = metadataSeries?.sequence ?: run {
    // This is super-duper hacky, but the API does not have a great way to
    // interpret the sequence int for books in a series. So if the data doesn't have any
    // then let's try to determine based on seriesName
    metadata.seriesName?.let { seriesName ->
      SERIES_SEQUENCE_REGEX.find(seriesName)?.groupValues?.getOrNull(1)?.toIntOrNull()
    } ?: fallbackSeriesSequence
  }

  return DatabaseMedia(
    libraryItemId = libraryItemId,

    mediaId = id,
    coverPath = coverPath,
    tags = tags,
    numTracks = numTracks,
    numAudioFiles = numAudioFiles,
    numChapters = numChapters,
    numMissingParts = numMissingParts,
    numInvalidAudioFiles = numInvalidAudioFiles,
    durationInMillis = duration?.seconds?.inWholeMilliseconds ?: run {
      // We've hit an odd response from the API, so we need to compute this on the fly
      val computedDuration = (this as? MediaExpanded)?.let {
        it.audioFiles
          .sumOf { it.duration.toDouble() }
          .seconds
      } ?: error("Unable to compute duration, breaking to debug")
      computedDuration.inWholeMilliseconds
    },
    sizeInBytes = size ?: run {
      (this as? MediaExpanded)?.let {
        it.audioFiles
          .sumOf { it.metadata.size }
          .toLong()
      } ?: error("Unable to compute size, breaking to debug")
    },
    propertySize = propertySize,
    ebookFormat = ebookFormat,

    metadata_title = metadata.title,
    metadata_subtitle = metadata.subtitle,
    metadata_genres = metadata.genres,
    metadata_publishedYear = metadata.publishedYear,
    metadata_publishedDate = metadata.publishedDate,
    metadata_publisher = metadata.publisher,
    metadata_description = metadata.description,
    metadata_isbn = metadata.isbn,
    metadata_asin = metadata.asin,
    metadata_language = metadata.language,
    metadata_explicit = metadata.explicit,
    metadata_abridged = metadata.abridged,
    metadata_titleIgnorePrefix = metadata.titleIgnorePrefix,
    metadata_authorName = metadataAuthorName,
    metadata_authorNameLF = metadataAuthorNameLF,
    metadata_narratorName = metadata.narratorName,
    metadata_seriesName = metadata.seriesName,

    metadata_series_id = metadataSeries?.id,
    metadata_series_name = metadataSeries?.name,
    metadata_series_sequence = metadataSeriesSequence,
  )
}

fun DomainMedia.asDbModel(
  libraryItemId: String,
): DatabaseMedia {
  val metadataAuthorName = metadata.authorName
  val metadataAuthorNameLF = metadata.authorNameLastFirst
  val metadataSeries = metadata.seriesSequence

  return DatabaseMedia(
    libraryItemId = libraryItemId,

    mediaId = id,
    coverPath = coverPath,
    tags = tags,
    numTracks = numTracks,
    numAudioFiles = numAudioFiles,
    numChapters = numChapters,
    numMissingParts = numMissingParts,
    numInvalidAudioFiles = numInvalidAudioFiles,
    durationInMillis = durationInMillis,
    sizeInBytes = sizeInBytes,
    propertySize = null,
    ebookFormat = ebookFormat,

    metadata_title = metadata.title,
    metadata_subtitle = metadata.subtitle,
    metadata_genres = metadata.genres,
    metadata_publishedYear = metadata.publishedYear,
    metadata_publishedDate = metadata.publishedDate,
    metadata_publisher = metadata.publisher,
    metadata_description = metadata.description,
    metadata_isbn = metadata.ISBN,
    metadata_asin = metadata.ASIN,
    metadata_language = metadata.language,
    metadata_explicit = metadata.isExplicit,
    metadata_abridged = metadata.isAbridged,
    metadata_titleIgnorePrefix = metadata.titleIgnorePrefix,
    metadata_authorName = metadataAuthorName,
    metadata_authorNameLF = metadataAuthorNameLF,
    metadata_narratorName = metadata.narratorName,
    metadata_seriesName = metadata.seriesName,

    metadata_series_id = metadataSeries?.id,
    metadata_series_name = metadataSeries?.name,
    metadata_series_sequence = metadataSeries?.sequence,
  )
}

private val String.lastFirst: String get() {
  val parts = split(" ")
  return if (parts.size > 1) {
    val firstName = parts.subList(0, parts.lastIndex).joinToString(" ")
    val lastName = parts.last()
    "$lastName, $firstName"
  } else {
    this
  }
}

suspend fun LibraryItemExpanded.asDomainModel(
  tokenHydrator: TokenHydrator,
): LibraryItem {
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
    mediaType = when (mediaType) {
      NetworkMediaType.Book -> DomainMediaType.Book
      NetworkMediaType.Podcast -> DomainMediaType.Podcast
      NetworkMediaType.Podcast2 -> DomainMediaType.Podcast
    },
    numFiles = numFiles ?: -1,
    sizeInBytes = size ?: -1,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = with(this.media) {
      DomainMedia(
        id = id,
        metadata = metadata.asDomainModel(),
        coverImageUrl = tokenHydrator.hydrateLibraryItem(id),
        coverPath = coverPath,
        tags = tags ?: emptyList(),
        numTracks = numTracks,
        numAudioFiles = numAudioFiles,
        numChapters = numChapters,
        numMissingParts = numMissingParts,
        numInvalidAudioFiles = numInvalidAudioFiles,
        durationInMillis = duration?.seconds?.inWholeMilliseconds ?: 0,
        sizeInBytes = size ?: -1,
        ebookFormat = ebookFormat,

        audioFiles = audioFiles.map {
          it.asDomainModel()
        },

        chapters = chapters.map {
          it.asDomainModel()
        },

        tracks = tracks.map {
          it.asDomainModel(tokenHydrator)
        },
      )
    },
  )
}

suspend fun SelectForSeries.asDomainModel(
  tokenHydrator: TokenHydrator,
): LibraryItem {
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
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = DomainMedia(
      id = mediaId,
      metadata = DomainMedia.Metadata(
        title = metadata_title,
        titleIgnorePrefix = metadata_titleIgnorePrefix,
        subtitle = metadata_subtitle,
        authorName = metadata_authorName,
        authorNameLastFirst = metadata_authorNameLF,
        narratorName = metadata_narratorName,
        seriesName = metadata_seriesName,
        genres = metadata_genres ?: emptyList(),
        publishedYear = metadata_publishedYear,
        publishedDate = metadata_publishedDate,
        publisher = metadata_publisher,
        description = metadata_description,
        ISBN = metadata_isbn,
        ASIN = metadata_asin,
        language = metadata_language,
        isExplicit = metadata_explicit,
        isAbridged = metadata_abridged,
        seriesSequence = createIfNotNull(
          metadata_series_id,
          metadata_series_name,
          metadata_series_sequence,
        ) {
          SeriesSequence(
            id = metadata_series_id!!,
            name = metadata_series_name!!,
            sequence = metadata_series_sequence!!,
          )
        },
      ),
      coverImageUrl = tokenHydrator.hydrateLibraryItem(id),
      coverPath = coverPath,
      tags = tags ?: emptyList(),
      numTracks = numTracks,
      numAudioFiles = numAudioFiles,
      numChapters = numChapters,
      numMissingParts = numMissingParts,
      numInvalidAudioFiles = numInvalidAudioFiles,
      durationInMillis = durationInMillis,
      sizeInBytes = sizeInBytes,
      ebookFormat = ebookFormat,
    ),
  )
}

suspend fun SelectForCollection.asDomainModel(
  tokenHydrator: TokenHydrator,
): LibraryItem {
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
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = DomainMedia(
      id = mediaId,
      metadata = DomainMedia.Metadata(
        title = metadata_title,
        titleIgnorePrefix = metadata_titleIgnorePrefix,
        subtitle = metadata_subtitle,
        authorName = metadata_authorName,
        authorNameLastFirst = metadata_authorNameLF,
        narratorName = metadata_narratorName,
        seriesName = metadata_seriesName,
        genres = metadata_genres ?: emptyList(),
        publishedYear = metadata_publishedYear,
        publishedDate = metadata_publishedDate,
        publisher = metadata_publisher,
        description = metadata_description,
        ISBN = metadata_isbn,
        ASIN = metadata_asin,
        language = metadata_language,
        isExplicit = metadata_explicit,
        isAbridged = metadata_abridged,
        seriesSequence = createIfNotNull(
          metadata_series_id,
          metadata_series_name,
          metadata_series_sequence,
        ) {
          SeriesSequence(
            id = metadata_series_id!!,
            name = metadata_series_name!!,
            sequence = metadata_series_sequence!!,
          )
        },
      ),
      coverImageUrl = tokenHydrator.hydrateLibraryItem(id),
      coverPath = coverPath,
      tags = tags ?: emptyList(),
      numTracks = numTracks,
      numAudioFiles = numAudioFiles,
      numChapters = numChapters,
      numMissingParts = numMissingParts,
      numInvalidAudioFiles = numInvalidAudioFiles,
      durationInMillis = durationInMillis,
      sizeInBytes = sizeInBytes,
      ebookFormat = ebookFormat,
    ),
  )
}

suspend fun SelectForAuthorName.asDomainModel(
  tokenHydrator: TokenHydrator,
): LibraryItem {
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
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = DomainMedia(
      id = mediaId,
      metadata = DomainMedia.Metadata(
        title = metadata_title,
        titleIgnorePrefix = metadata_titleIgnorePrefix,
        subtitle = metadata_subtitle,
        authorName = metadata_authorName,
        authorNameLastFirst = metadata_authorNameLF,
        narratorName = metadata_narratorName,
        seriesName = metadata_seriesName,
        genres = metadata_genres ?: emptyList(),
        publishedYear = metadata_publishedYear,
        publishedDate = metadata_publishedDate,
        publisher = metadata_publisher,
        description = metadata_description,
        ISBN = metadata_isbn,
        ASIN = metadata_asin,
        language = metadata_language,
        isExplicit = metadata_explicit,
        isAbridged = metadata_abridged,
        seriesSequence = createIfNotNull(
          metadata_series_id,
          metadata_series_name,
          metadata_series_sequence,
        ) {
          SeriesSequence(
            id = metadata_series_id!!,
            name = metadata_series_name!!,
            sequence = metadata_series_sequence!!,
          )
        },
      ),
      coverImageUrl = tokenHydrator.hydrateLibraryItem(id),
      coverPath = coverPath,
      tags = tags ?: emptyList(),
      numTracks = numTracks,
      numAudioFiles = numAudioFiles,
      numChapters = numChapters,
      numMissingParts = numMissingParts,
      numInvalidAudioFiles = numInvalidAudioFiles,
      durationInMillis = durationInMillis,
      sizeInBytes = sizeInBytes,
      ebookFormat = ebookFormat,
    ),
  )
}

suspend fun LibraryItemWithMedia.asDomainModel(
  tokenHydrator: TokenHydrator,
  mediaAudioFiles: List<MediaAudioFiles> = emptyList(),
  mediaAudioTracks: List<MediaAudioTracks> = emptyList(),
  mediaChapters: List<MediaChapters> = emptyList(),
  metadataAuthors: List<MetadataAuthor> = emptyList(),
): LibraryItem {
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
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = DomainMedia(
      id = mediaId,
      metadata = DomainMedia.Metadata(
        title = metadata_title,
        titleIgnorePrefix = metadata_titleIgnorePrefix,
        subtitle = metadata_subtitle,
        authorName = metadata_authorName ?: metadataAuthors.firstOrNull()?.name,
        authorNameLastFirst = metadata_authorNameLF ?: metadataAuthors.firstOrNull()?.name?.lastFirst,
        narratorName = metadata_narratorName,
        seriesName = metadata_seriesName,
        genres = metadata_genres ?: emptyList(),
        publishedYear = metadata_publishedYear,
        publishedDate = metadata_publishedDate,
        publisher = metadata_publisher,
        description = metadata_description,
        ISBN = metadata_isbn,
        ASIN = metadata_asin,
        language = metadata_language,
        isExplicit = metadata_explicit,
        isAbridged = metadata_abridged,
        seriesSequence = createIfNotNull(
          metadata_series_id,
          metadata_series_name,
          metadata_series_sequence,
        ) {
          SeriesSequence(
            id = metadata_series_id!!,
            name = metadata_series_name!!,
            sequence = metadata_series_sequence!!,
          )
        },
        authors = metadataAuthors.map {
          DomainMedia.AuthorMetadata(
            id = it.id,
            name = it.name,
          )
        },
      ),
      audioFiles = mediaAudioFiles.map {
        AudioFile(
          index = it.mediaIndex,
          ino = it.ino,
          addedAt = it.addedAt,
          updatedAt = it.updatedAt,
          trackNumFromMeta = it.trackNumFromMeta,
          discNumFromMeta = it.discNumFromMeta,
          trackNumFromFilename = it.trackNumFromFilename,
          discNumFromFilename = it.discNumFromFilename,
          manuallyVerified = it.manuallyVerified,
          invalid = it.invalid,
          exclude = it.exclude,
          error = it.error,
          format = it.format ?: "",
          duration = it.duration.seconds,
          bitRate = it.bitRate,
          language = it.language,
          codec = it.codec,
          timeBase = it.timeBase,
          channels = it.channels,
          channelLayout = it.channelLayout,
          embeddedCoverArt = it.embeddedCoverArt,
          mimeType = it.mimeType,
        )
      },
      chapters = mediaChapters.map {
        Chapter(
          id = it.id,
          start = it.start.toFloat(),
          end = it.end.toFloat(),
          title = it.title,
        )
      },
      tracks = mediaAudioTracks.map {
        AudioTrack(
          index = it.mediaIndex,
          startOffset = it.startOffset.toFloat(),
          duration = it.duration.toFloat(),
          title = it.title,
          contentUrl = tokenHydrator.hydrateUrl(it.contentUrl),
          contentUrlWithToken = tokenHydrator.hydrateUrlWithToken(it.contentUrl),
          mimeType = it.mimeType,
          codec = it.codec,
          metadata = FileMetadata(
            filename = it.metadata_filename,
            ext = it.metadata_ext,
            path = tokenHydrator.hydrateUrl(it.metadata_path),
            relPath = it.metadata_relPath,
            size = it.metadata_size,
            mtimeMs = it.metadata_mtimeMs,
            ctimeMs = it.metadata_ctimeMs,
            birthtimeMs = it.metadata_birthtimeMs,
          ),
        )
      },
      coverImageUrl = tokenHydrator.hydrateLibraryItem(id),
      coverPath = coverPath,
      tags = tags ?: emptyList(),
      numTracks = numTracks,
      numAudioFiles = numAudioFiles,
      numChapters = numChapters,
      numMissingParts = numMissingParts,
      numInvalidAudioFiles = numInvalidAudioFiles,
      durationInMillis = durationInMillis,
      sizeInBytes = sizeInBytes,
      ebookFormat = ebookFormat,
    ),
    userMediaProgress = null,
  )
}
