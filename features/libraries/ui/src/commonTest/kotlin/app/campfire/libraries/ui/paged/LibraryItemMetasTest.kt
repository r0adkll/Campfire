// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import app.campfire.common.compose.widgets.LibraryItemMeta
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.core.settings.ContentSortMode
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours

class LibraryItemMetasTest {

  private val length = LibraryItemMeta.Length(10.hours)
  private val audiobook = libraryItem(duration = 10.hours, publishedYear = "2019")
  private val ebook = libraryItem(duration = 10.hours, numTracks = 0, ebookFormat = "epub")
  private val size = LibraryItemMeta.Size(1L * 1024L * 1024L)

  @Test
  fun `an audiobook shows its length`() {
    assertThat(audiobook.listMeta(ContentSortMode.Title, null)).containsExactly(length)
  }

  @Test
  fun `an ebook shows its file size instead of a length`() {
    assertThat(ebook.listMeta(ContentSortMode.Title, null)).containsExactly(size)
  }

  @Test
  fun `a podcast shows its episode count`() {
    val podcast = audiobook.copy(
      media = Media.Podcast(
        id = "podcast",
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
        numEpisodes = 12,
      ),
    )

    assertThat(podcast.listMeta(ContentSortMode.Title, null))
      .containsExactly(LibraryItemMeta.Episodes(12))
  }

  @Test
  fun `sorts already visible on the item add nothing`() {
    listOf(ContentSortMode.Title, ContentSortMode.AuthorFL, ContentSortMode.AuthorLF).forEach { mode ->
      assertThat(audiobook.listMeta(mode, null)).containsExactly(length)
    }
  }

  @Test
  fun `sorting by publish year shows the year`() {
    assertThat(audiobook.listMeta(ContentSortMode.PublishYear, null))
      .containsExactly(length, LibraryItemMeta.PublishedYear("2019"))
  }

  @Test
  fun `sorting by publish year skips items without one`() {
    assertThat(libraryItem(duration = 10.hours, publishedYear = " ").listMeta(ContentSortMode.PublishYear, null))
      .containsExactly(length)
  }

  @Test
  fun `sorting by size shows the size`() {
    assertThat(audiobook.listMeta(ContentSortMode.Size, null)).containsExactly(length, size)
  }

  @Test
  fun `sorting an ebook by size does not repeat its size`() {
    assertThat(ebook.listMeta(ContentSortMode.Size, null)).containsExactly(size)
  }

  @Test
  fun `sorting by duration does not repeat the length`() {
    assertThat(audiobook.listMeta(ContentSortMode.Duration, null)).containsExactly(length)
    assertThat(ebook.listMeta(ContentSortMode.Duration, null)).containsExactly(size)
  }

  @Test
  fun `sorting by date added shows when it was added`() {
    assertThat(audiobook.copy(addedAtMillis = 1234L).listMeta(ContentSortMode.AddedAt, null))
      .containsExactly(length, LibraryItemMeta.AddedAt(1234L))
  }

  @Test
  fun `filtering by series shows the position in that series`() {
    val item = libraryItem(
      duration = 10.hours,
      seriesSequence = SeriesSequence(id = "dcc", name = "DCC", sequence = 3.0),
    )

    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Series("dcc", "DCC")))
      .containsExactly(length, LibraryItemMeta.SeriesPosition("3"))
  }

  @Test
  fun `filtering by a series without a known position adds nothing`() {
    val item = libraryItem(
      duration = 10.hours,
      seriesSequence = SeriesSequence(id = "dcc", name = "DCC", sequence = SeriesSequence.UNKNOWN_SEQUENCE),
    )

    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Series("dcc", "DCC"))).containsExactly(length)
    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Series("other", "Other"))).containsExactly(length)
  }

  @Test
  fun `filtering by in progress shows the time left`() {
    val item = libraryItem(
      duration = 10.hours,
      userMediaProgress = mediaProgress(progress = 0.25f, duration = 10.hours),
    )

    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Progress(ContentFilter.Progress.Type.InProgress)))
      .containsExactly(length, LibraryItemMeta.Remaining(7.5.hours))
  }

  @Test
  fun `filtering an ebook by in progress shows no time left`() {
    val item = ebook.copy(userMediaProgress = mediaProgress())

    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Progress(ContentFilter.Progress.Type.InProgress)))
      .containsExactly(size)
  }

  @Test
  fun `filtering by finished shows when it was finished`() {
    val item = libraryItem(
      duration = 10.hours,
      userMediaProgress = mediaProgress(isFinished = true).copy(finishedAt = 5678L),
    )

    assertThat(item.listMeta(ContentSortMode.Title, ContentFilter.Progress(ContentFilter.Progress.Type.Finished)))
      .containsExactly(length, LibraryItemMeta.FinishedAt(5678L))
  }

  @Test
  fun `filtering by not started adds nothing`() {
    assertThat(
      audiobook.listMeta(ContentSortMode.Title, ContentFilter.Progress(ContentFilter.Progress.Type.NotStarted)),
    )
      .containsExactly(length)
  }

  @Test
  fun `filtering by tracks shows the track count`() {
    assertThat(audiobook.listMeta(ContentSortMode.Title, ContentFilter.Tracks(ContentFilter.Tracks.Type.Multi)))
      .containsExactly(length, LibraryItemMeta.Tracks(10))
  }

  @Test
  fun `filters shared by every result add nothing`() {
    assertThat(audiobook.listMeta(ContentSortMode.Title, ContentFilter.Genres("Sci-fi"))).containsExactly(length)
  }

  @Test
  fun `sort and filter facts follow the default in order`() {
    val item = libraryItem(
      duration = 10.hours,
      publishedYear = "2020",
      seriesSequence = SeriesSequence(id = "dcc", name = "DCC", sequence = 2.0),
    )

    assertThat(item.listMeta(ContentSortMode.PublishYear, ContentFilter.Series("dcc", "DCC")))
      .containsExactly(length, LibraryItemMeta.PublishedYear("2020"), LibraryItemMeta.SeriesPosition("2"))
  }
}
