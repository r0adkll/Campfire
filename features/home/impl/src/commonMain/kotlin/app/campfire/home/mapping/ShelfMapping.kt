package app.campfire.home.mapping

import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.model.Author as DomainAuthor
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesSequence
import app.campfire.home.api.model.Shelf as DomainShelf
import app.campfire.home.progress.MediaProgressDataSource
import app.campfire.network.models.Author as NetworkAuthor
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.SeriesPersonalized
import app.campfire.network.models.Shelf
import app.campfire.network.models.Shelf as NetworkShelf
import kotlin.time.Duration.Companion.seconds

suspend fun NetworkShelf.asDomainModel(
  imageHydrator: CoverImageHydrator,
  mediaProgressDataSource: MediaProgressDataSource,
): DomainShelf<*> {
  return DomainShelf(
    id = id,
    label = label,
    total = total,
    entities = when (this) {
      is Shelf.BookShelf -> entities.map { it.asDomainModel(imageHydrator, mediaProgressDataSource) }
      is Shelf.EpisodeShelf -> entities.map { it.asDomainModel(imageHydrator, mediaProgressDataSource) }
      is Shelf.PodcastShelf -> entities.map { it.asDomainModel(imageHydrator, mediaProgressDataSource) }
      is Shelf.SeriesShelf -> entities.map { it.asDomainModel(imageHydrator, mediaProgressDataSource) }
      is Shelf.AuthorShelf -> entities.map { it.asDomainModel(imageHydrator) }
    },
  )
}

suspend fun LibraryItemMinified<*>.asDomainModel(
  imageHydrator: CoverImageHydrator,
  mediaProgressDataSource: MediaProgressDataSource,
): LibraryItem {
  return LibraryItem(
    id = id,
    libraryId = libraryId,
    isMissing = isMissing,
    isInvalid = isInvalid,
    mediaType = when (mediaType) {
      NetworkMediaType.Book -> MediaType.Book
      NetworkMediaType.Podcast -> MediaType.Podcast
    },
    numFiles = numFiles ?: libraryFiles?.size ?: -1,
    sizeInBytes = size,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    userMediaProgress = mediaProgressDataSource.getMediaProgress(id),
    media = Media(
      id = media.id,
      coverImageUrl = imageHydrator.hydrateLibraryItem(id),
      coverPath = media.coverPath,
      tags = media.tags ?: emptyList(),
      numTracks = media.numTracks,
      numChapters = media.numChapters,
      numMissingParts = media.numMissingParts,
      numInvalidAudioFiles = media.numInvalidAudioFiles,
      numAudioFiles = media.numAudioFiles,
      durationInMillis = media.duration.seconds.inWholeMilliseconds,
      sizeInBytes = media.size,
      ebookFormat = media.ebookFormat,
      metadata = Media.Metadata(
        title = media.metadata.title,
        titleIgnorePrefix = media.metadata.titleIgnorePrefix,
        subtitle = media.metadata.subtitle,
        authorName = media.metadata.authorName,
        authorNameLastFirst = media.metadata.authorNameLF,
        narratorName = media.metadata.narratorName,
        seriesName = media.metadata.seriesName,
        genres = media.metadata.genres ?: emptyList(),
        publishedYear = media.metadata.publishedYear,
        publishedDate = media.metadata.publishedDate,
        publisher = media.metadata.publisher,
        description = media.metadata.description,
        ISBN = media.metadata.isbn,
        ASIN = media.metadata.asin,
        language = media.metadata.language,
        isExplicit = media.metadata.explicit,
        isAbridged = media.metadata.abridged,
        seriesSequence = (media.metadata as? MinifiedBookMetadata)?.series?.let {
          SeriesSequence(
            id = it.id,
            name = it.name,
            sequence = it.sequence,
          )
        },
      ),
    ),
  )
}

suspend fun SeriesPersonalized.asDomainModel(
  imageHydrator: CoverImageHydrator,
  mediaProgressDataSource: MediaProgressDataSource,
): Series {
  return Series(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    books = books?.map { it.asDomainModel(imageHydrator, mediaProgressDataSource) },
    inProgress = inProgress == true,
    hasActiveBook = hasActiveBook == true,
    hideFromContinueListening = hideFromContinueListening == true,
    bookInProgressLastUpdate = bookInProgressLastUpdate,
    firstBookUnread = firstBookUnread?.asDomainModel(imageHydrator, mediaProgressDataSource),
  )
}

suspend fun NetworkAuthor.asDomainModel(
  imageHydrator: CoverImageHydrator,
): DomainAuthor {
  return DomainAuthor(
    id = id,
    asin = asin,
    name = name,
    description = description,
    imagePath = imageHydrator.hydrateAuthor(id),
    addedAt = addedAt,
    updatedAt = updatedAt,
    numBooks = numBooks,
  )
}
