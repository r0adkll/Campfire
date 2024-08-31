package app.campfire.data.mapping

import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaMinified
import app.campfire.core.model.MediaType as DomainMediaType
import app.campfire.data.LibraryItem as DatabaseLibraryItem
import app.campfire.data.Media as DatabaseMedia
import app.campfire.data.SelectForLibrary
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaMinified as NetworkMediaMinified
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.core.model.SeriesSequence
import app.campfire.core.util.createIfNotNull
import app.campfire.data.SelectForCollection
import app.campfire.data.SelectForSeries
import app.campfire.network.models.MinifiedBookMetadata
import kotlin.time.Duration.Companion.seconds

fun LibraryItemMinified<*>.asDbModels(
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
    numFiles = numFiles ?: libraryFiles?.size ?: -1,
    size = size,
    weight = weight,
    progressLastUpdate = progressLastUpdate,
    finishedAt = finishedAt,
    prevBookInProgressLastUpdate = prevBookInProgressLastUpdate,
    serverUrl = serverUrl,
  ) to media.asDbModel(id)
}

fun NetworkMediaMinified<*>.asDbModel(
  libraryItemId: String,
): DatabaseMedia {
  val metadataSeries = (metadata as? MinifiedBookMetadata)?.series
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

    metadata_series_id = metadataSeries?.id,
    metadata_series_name = metadataSeries?.name,
    metadata_series_sequence = metadataSeries?.sequence,
  )
}

suspend fun SelectForLibrary.asDomainModel(
  coverImageHydrator: CoverImageHydrator,
): LibraryItem {
  return LibraryItem(
    id = id,
    libraryId = libraryId,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = MediaMinified(
      id = mediaId,
      metadata = MediaMinified.Metadata(
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
        }
      ),
      coverImageUrl = coverImageHydrator.hydrateLibraryItem(id),
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

suspend fun SelectForSeries.asDomainModel(
  coverImageHydrator: CoverImageHydrator,
): LibraryItem {
  return LibraryItem(
    id = id,
    libraryId = libraryId,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = MediaMinified(
      id = mediaId,
      metadata = MediaMinified.Metadata(
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
        }
      ),
      coverImageUrl = coverImageHydrator.hydrateLibraryItem(id),
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
  coverImageHydrator: CoverImageHydrator,
): LibraryItem {
  return LibraryItem(
    id = id,
    libraryId = libraryId,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = mediaType,
    numFiles = numFiles,
    sizeInBytes = sizeInBytes,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    media = MediaMinified(
      id = mediaId,
      metadata = MediaMinified.Metadata(
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
        }
      ),
      coverImageUrl = coverImageHydrator.hydrateLibraryItem(id),
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
