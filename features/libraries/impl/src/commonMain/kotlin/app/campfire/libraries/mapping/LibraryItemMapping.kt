package app.campfire.libraries.mapping

import app.campfire.core.model.MediaType as DomainMediaType
import app.campfire.data.LibraryItem as DatabaseLibraryItem
import app.campfire.data.Media as DatabaseMedia
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaMinified as NetworkMediaMinified
import app.campfire.network.models.MediaType as NetworkMediaType
import kotlin.time.Duration.Companion.seconds

fun LibraryItemMinified.asDbModels(
  serverUrl: String,
): Pair<DatabaseLibraryItem, DatabaseMedia> {
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
    },
    numFiles = numFiles,
    size = size,
    weight = weight,
    progressLastUpdate = progressLastUpdate,
    finishedAt = finishedAt,
    prevBookInProgressLastUpdate = prevBookInProgressLastUpdate,
    serverUrl = serverUrl,
  ) to media.asDbModel(id)
}

fun NetworkMediaMinified.asDbModel(
  libraryItemId: String,
): DatabaseMedia {
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
    durationInMillis = duration.seconds.inWholeMilliseconds,
    sizeInBytes = size,
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
    metadata_authorName = metadata.authorName,
    metadata_authorNameLF = metadata.authorNameLF,
    metadata_narratorName = metadata.narratorName,
    metadata_seriesName = metadata.seriesName,

    metadata_series_id = metadata.series?.id,
    metadata_series_name = metadata.series?.name,
    metadata_series_sequence = metadata.series?.sequence,
  )
}
