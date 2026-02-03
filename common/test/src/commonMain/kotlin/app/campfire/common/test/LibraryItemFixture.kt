package app.campfire.home.ui

import app.campfire.core.extensions.asSeconds
import app.campfire.core.model.AudioFile
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryFile
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.MediaId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.SeriesSequence
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Creates a fake [LibraryItem] for use in tests.
 *
 * @param id The unique identifier for the library item.
 * @param ino The inode number of the file/directory.
 * @param libraryId The ID of the library this item belongs to.
 * @param oldLibraryId An optional old library ID.
 * @param folderId The ID of the folder containing this item.
 * @param path The absolute path to the item.
 * @param relPath The relative path to the item within the library.
 * @param isFile Whether the item is a file or a directory.
 * @param mtimeMs The modification time in milliseconds.
 * @param ctimeMs The creation time in milliseconds.
 * @param birthtimeMs The birth time in milliseconds.
 * @param isMissing Whether the item is marked as missing.
 * @param isInvalid Whether the item is marked as invalid.
 * @param mediaType The type of media (e.g., Audio, Video).
 * @param numFiles The number of files (if it's a directory).
 * @param sizeInBytes The total size in bytes.
 * @param addedAtMillis The timestamp when the item was added to the library.
 * @param updatedAtMillis The timestamp when the item was last updated.
 * @param media The detailed media information.
 * @param libraryFiles A list of associated library files.
 * @param userMediaProgress The user's progress in the media.
 * @return A new [LibraryItem] instance with the specified or default fake data.
 */
fun libraryItem(
  id: LibraryItemId = "fakeItemId_${Random.nextInt()}",
  ino: String = "fakeIno_${Random.nextInt()}",
  libraryId: LibraryId = "fakeLibraryId",
  oldLibraryId: String? = null,
  folderId: String = "fakeFolderId",
  path: String = "/path/to/fake/item",
  relPath: String = "fake/item",
  isFile: Boolean = true,
  mtimeMs: Long = Clock.System.now().toEpochMilliseconds(),
  ctimeMs: Long = Clock.System.now().toEpochMilliseconds(),
  birthtimeMs: Long = Clock.System.now().toEpochMilliseconds(),
  isMissing: Boolean = false,
  isInvalid: Boolean = false,
  mediaType: MediaType = MediaType.Book,
  numFiles: Int = 1,
  sizeInBytes: Long = 1024L * 1024L, // 1 MB
  addedAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
  updatedAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
  media: Media = media(),
  libraryFiles: List<LibraryFile> = emptyList(),
  userMediaProgress: MediaProgress? = null,
): LibraryItem {
  return LibraryItem(
    id = id,
    ino = ino,
    libraryId = libraryId,
    oldLibraryId = oldLibraryId,
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
    addedAtMillis = addedAtMillis,
    updatedAtMillis = updatedAtMillis,
    media = media,
    libraryFiles = libraryFiles,
    userMediaProgress = userMediaProgress,
  )
}

// Helper function to create a fake Media.Metadata object
fun mediaMetadata(
  title: String? = "The Great Fake Adventure",
  titleIgnorePrefix: String? = "The",
  subtitle: String? = "A Test Saga",
  authorName: String? = "Dr. Fakenstein",
  authorNameLastFirst: String? = "Fakenstein, Dr.",
  narratorName: String? = "Voice Actor Prime",
  seriesName: String? = "The Test Chronicles",
  seriesSequence: SeriesSequence? = SeriesSequence(
    id = "1234",
    name = "Test Series",
    sequence = 0,
  ),
  genres: List<String> = listOf("Fantasy", "Testing"),
  publishedYear: String? = "2024",
  publishedDate: String? = "2024-10-26",
  publisher: String? = "Fake House Publishing",
  description: String? = "A thrilling tale of bits and bytes.",
  ISBN: String? = "978-3-16-148410-0",
  ASIN: String? = "B002RI9Z9E",
  language: String? = "en-US",
  isExplicit: Boolean = false,
  isAbridged: Boolean = false,
  authors: List<Media.AuthorMetadata> = listOf(authorMetadata()),
  narrators: List<String> = listOf("Voice Actor Prime"),
): Media.Metadata {
  return Media.Metadata(
    title = title,
    titleIgnorePrefix = titleIgnorePrefix,
    subtitle = subtitle,
    authorName = authorName,
    authorNameLastFirst = authorNameLastFirst,
    narratorName = narratorName,
    seriesName = seriesName,
    seriesSequence = seriesSequence,
    genres = genres,
    publishedYear = publishedYear,
    publishedDate = publishedDate,
    publisher = publisher,
    description = description,
    ISBN = ISBN,
    ASIN = ASIN,
    language = language,
    isExplicit = isExplicit,
    isAbridged = isAbridged,
    authors = authors,
    narrators = narrators,
  )
}

// Helper function to create a fake Media.AuthorMetadata object
fun authorMetadata(
  id: String = "author_${Random.nextInt()}",
  name: String = "Dr. Fakenstein",
): Media.AuthorMetadata {
  return Media.AuthorMetadata(id = id, name = name)
}

/**
 * Creates a fake [Media] object for use in tests.
 * Allows for overriding specific fields to suit different test scenarios.
 */
fun media(
  id: MediaId = "media_${Random.nextInt()}",
  metadata: Media.Metadata = mediaMetadata(),
  coverImageUrl: String = "https://example.com/cover.jpg",
  coverPath: String? = "/path/to/local/cover.jpg",
  tags: List<String> = listOf("tag1", "tag2"),
  numTracks: Int = 1,
  numAudioFiles: Int = 1,
  numChapters: Int = 5,
  numMissingParts: Int = 0,
  numInvalidAudioFiles: Int = 0,
  durationInMillis: Long = 3600_000, // 1 hour
  sizeInBytes: Long = 100_000_000, // 100 MB
  ebookFormat: String? = null,
  audioFiles: List<AudioFile> = emptyList(),
  chapters: List<Chapter> = emptyList(),
  tracks: List<AudioTrack> = emptyList(),
): Media {
  return Media(
    id = id,
    metadata = metadata,
    coverImageUrl = coverImageUrl,
    coverPath = coverPath,
    tags = tags,
    numTracks = numTracks,
    numAudioFiles = numAudioFiles,
    numChapters = numChapters,
    numMissingParts = numMissingParts,
    numInvalidAudioFiles = numInvalidAudioFiles,
    durationInMillis = durationInMillis,
    sizeInBytes = sizeInBytes,
    ebookFormat = ebookFormat,
    audioFiles = audioFiles,
    chapters = chapters,
    tracks = tracks,
  )
}

fun chapter(
  id: Int = 0,
  start: Duration = Duration.ZERO,
  end: Duration = 5.minutes,
  title: String = "Chapter $id",
) = Chapter(
  id = id,
  start = start.asSeconds(),
  end = end.asSeconds(),
  title = title,
)

class LibraryItemBuilder {

  private var media = media()

  @LibraryItemDsl
  fun media(block: MediaBuilder.() -> Unit) {
    val builder = MediaBuilder()
    builder.block()
    media = builder.build()
  }

  internal fun build(): LibraryItem {
    return libraryItem(
      media = media,
    )
  }
}

class MediaBuilder {

  var duration: Duration = Duration.ZERO

  private var metadata = mediaMetadata()

  @LibraryItemDsl
  fun metadata(block: MetadataBuilder.() -> Unit) {
    val builder = MetadataBuilder()
    builder.block()
    metadata = builder.build()
  }

  internal fun build(): Media {
    return media(
      durationInMillis = duration.inWholeMilliseconds,
      metadata = metadata,
    )
  }
}

class MetadataBuilder {

  var title: String? = "The Great Fake Adventure"
  var subtitle: String? = "A Test Saga"
  var authorName: String? = "Dr. Fakenstein"
  var narratorName: String? = "Voice Actor Prime"
  var authors: List<Media.AuthorMetadata> = listOf(authorMetadata())
  var narrators: List<String> = emptyList()
  var seriesName: String? = null
  var seriesSequence: SeriesSequence? = SeriesSequence("1234", "Test Series", 0)

  internal fun build(): Media.Metadata {
    return mediaMetadata(
      title = title,
      subtitle = subtitle,
      authorName = authorName,
      narratorName = narratorName,
      authors = authors,
      narrators = narrators,
      seriesName = seriesName,
      seriesSequence = seriesSequence,
    )
  }
}

@LibraryItemDsl
fun libraryItem(block: LibraryItemBuilder.() -> Unit): LibraryItem {
  val builder = LibraryItemBuilder()
  builder.block()
  return builder.build()
}

@DslMarker
annotation class LibraryItemDsl
