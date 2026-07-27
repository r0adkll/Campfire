package app.campfire.core.model

import app.campfire.core.model.preview.libraryItem
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class MediaEbookOnlyTest {

  @Test
  fun ebookOnlyBook_isEbookOnly() {
    val item = libraryItem(numTracks = 0, numOfChapters = 0, ebookFormat = "epub")

    assertThat(item.isEbookOnly).isTrue()
    assertThat(item.media.isEbookOnly).isTrue()
  }

  @Test
  fun bookWithAudioAndEbook_isNotEbookOnly() {
    val item = libraryItem(numTracks = 10, ebookFormat = "epub")

    assertThat(item.isEbookOnly).isFalse()
  }

  @Test
  fun bookWithAudioOnly_isNotEbookOnly() {
    val item = libraryItem(numTracks = 10, ebookFormat = null)

    assertThat(item.isEbookOnly).isFalse()
  }

  @Test
  fun bookWithNoTracksAndNoEbook_isNotEbookOnly() {
    val item = libraryItem(numTracks = 0, numOfChapters = 0, ebookFormat = null)

    assertThat(item.isEbookOnly).isFalse()
  }

  @Test
  fun podcast_isNotEbookOnly() {
    val podcast = Media.Podcast(
      id = "podcast_media_id",
      metadata = Media.Metadata.Podcast(
        title = "Podcast",
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
      ),
      coverImageUrl = "",
      coverPath = null,
      tags = emptyList(),
      sizeInBytes = 0L,
    )

    assertThat(podcast.isEbookOnly).isFalse()
  }
}
