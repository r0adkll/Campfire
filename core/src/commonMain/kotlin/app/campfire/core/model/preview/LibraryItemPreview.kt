package app.campfire.core.model.preview

import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.SeriesSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

val previewLibraryItemDuration = (13.hours + 34.minutes + 56.seconds)

fun libraryItem(
  id: LibraryItemId = "preview_item_id",
  description: String? = "A man. His ex-girlfriend's cat. A sadistic game show unlike anything in the " +
    "universe: a dungeon crawl where survival depends on killing your prey in the most " +
    "entertaining way possible. In a flash, every human-erected construction on Earth - " +
    "from Buckingham Palace to the tiniest of sheds - collapses in a heap, sinking into the ground. " +
    "The buildings and all the people inside have all been atomized and transformed into the " +
    "dungeon: an 18-level labyrinth filled with traps, monsters, and loot.",
  duration: Duration = previewLibraryItemDuration,
  publisher: String? = "Soundbooth Theater",
  publishedYear: String? = "2021",
  genres: List<String> = listOf("Sci-fi", "LitRPG"),
  tags: List<String> = listOf("Lit", "RPG", "NoPants", "Tootsies"),
  seriesSequence: SeriesSequence? = null,
  numOfChapters: Int = 20,
) = LibraryItem(
  id = id,
  ino = "",
  libraryId = "preview_library_id",
  folderId = "",
  path = "",
  relPath = "",
  isFile = false,
  mtimeMs = 0L,
  ctimeMs = 0L,
  birthtimeMs = 0L,
  isMissing = false,
  isInvalid = false,
  mediaType = MediaType.Book,
  numFiles = 10,
  sizeInBytes = 1L * 1024L * 1024L,
  addedAtMillis = 0L,
  updatedAtMillis = 0L,
  media = Media(
    id = "preview_media_id",
    metadata = Media.Metadata(
      title = "Dungeon Crawler Carl",
      titleIgnorePrefix = null,
      subtitle = "Dungeon Crawler Carl, Book 1",
      authorName = "Matt Dinnaman",
      authorNameLastFirst = null,
      narratorName = "Jeff Hays",
      seriesName = "Dungeon Crawler Carl",
      seriesSequence = seriesSequence,
      genres = genres,
      publishedYear = publishedYear,
      publishedDate = null,
      publisher = publisher,
      description = description,
      ISBN = null,
      language = "English",
      ASIN = null,
      isExplicit = true,
      isAbridged = false,
    ),
    coverImageUrl = "",
    coverPath = null,
    tags = tags,
    numTracks = 10,
    numAudioFiles = 10,
    numChapters = 23,
    numMissingParts = 0,
    numInvalidAudioFiles = 0,
    durationInMillis = duration.inWholeMilliseconds,
    sizeInBytes = 1L * 1024L * 1024L,
    chapters = (0 until numOfChapters).map { chapter ->
      val durationPerChapter = duration / numOfChapters
      val chapterStart = (durationPerChapter * chapter).toDouble(DurationUnit.SECONDS).toFloat()
      val chapterEnd = (chapterStart + durationPerChapter.toDouble(DurationUnit.SECONDS).toFloat())

      Chapter(
        id = chapter,
        start = chapterStart,
        end = chapterEnd,
        title = "Chapter ${chapter + 1}",
      )
    },
  ),
)
