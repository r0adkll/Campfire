// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import app.campfire.common.test.mediaProgress
import app.campfire.common.test.user
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.User
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/** Which quick actions an item's card offers, as the item and the account stand. */
class QuickActionTest {

  private val book = libraryItem(id = "book", isFile = false)
  private val ebook = libraryItem(id = "ebook", isFile = false, media = media(numTracks = 0, ebookFormat = "epub"))
  private val podcast = book.copy(
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
    ),
  )

  @Test
  fun aBookNotStartedIsOfferedToPlayDownloadFinishQueueAndAddToAPlaylist() {
    assertThat(actions(book, listener())).containsExactly(
      QuickAction.Play(resume = false),
      QuickAction.Download,
      QuickAction.MarkFinished,
      QuickAction.AddToQueue,
      QuickAction.AddToPlaylist,
    )
  }

  @Test
  fun aStartedBookResumes_andAFinishedOneIsOfferedToBeNotFinished() {
    assertThat(actions(book, listener(), progress = mediaProgress(book.id, progress = 0.4f)).first())
      .isEqualTo(QuickAction.Play(resume = true))

    val finished = actions(book, listener(), progress = mediaProgress(book.id, progress = 1f, isFinished = true))
    assertThat(finished.first()).isEqualTo(QuickAction.Play(resume = false))
    assertThat(QuickAction.MarkNotFinished in finished).isTrue()
    assertThat(QuickAction.MarkFinished in finished).isFalse()
  }

  @Test
  fun anEbookIsReadAndNeverQueued() {
    assertThat(actions(ebook, listener())).containsExactly(
      QuickAction.Read(resume = false),
      QuickAction.Download,
      QuickAction.MarkFinished,
      QuickAction.AddToPlaylist,
    )
  }

  @Test
  fun theDownloadFollowsItsState() {
    fun download(state: Download.State) = Download(DownloadContent.Audiobook(book.id), state)

    assertThat(QuickAction.RemoveDownload in actions(book, listener(), download = download(Download.State.Downloaded)))
      .isTrue()
    assertThat(QuickAction.StopDownload in actions(book, listener(), download = download(Download.State.Downloading)))
      .isTrue()
    // A failed one can be tried again, or without the permission, cleared away
    assertThat(QuickAction.Download in actions(book, listener(), download = download(Download.State.Failed)))
      .isTrue()
    assertThat(
      QuickAction.RemoveDownload in
        actions(book, listener(download = false), download = download(Download.State.Failed)),
    ).isTrue()
  }

  @Test
  fun withoutTheDownloadPermissionNothingIsDownloaded() {
    assertThat(actions(book, listener(download = false)).none { it == QuickAction.Download }).isTrue()
  }

  @Test
  fun whatsPlayingIsntOfferedToPlayOrQueue() {
    val offered = actions(book, listener(), isPlaying = true)

    assertThat(offered).containsExactly(QuickAction.Download, QuickAction.MarkFinished, QuickAction.AddToPlaylist)
  }

  @Test
  fun aQueuedBookCanComeOffTheQueue() {
    assertThat(QuickAction.RemoveFromQueue in actions(book, listener(), isQueued = true)).isTrue()
  }

  @Test
  fun anAdminCanAlsoAddToACollection() {
    assertThat(actions(book, listener(type = User.Type.Admin)).last()).isEqualTo(QuickAction.AddToCollection)
  }

  @Test
  fun aPodcastOffersOnlyItsCollections() {
    assertThat(actions(podcast, listener())).isEmpty()
    assertThat(actions(podcast, listener(type = User.Type.Admin))).containsExactly(QuickAction.AddToCollection)
  }

  @Test
  fun anyBookCanBeActedOn_butAPodcastOnlyWithSomethingToDoToIt() {
    assertThat(listener().canActOn(book)).isTrue()
    assertThat(listener().canActOn(podcast)).isFalse()
    assertThat(listener(type = User.Type.Admin).canActOn(podcast)).isTrue()
  }

  private fun actions(
    item: LibraryItem,
    user: User,
    progress: MediaProgress? = null,
    download: Download? = null,
    isQueued: Boolean = false,
    isPlaying: Boolean = false,
  ) = quickActions(item, user, progress, download, isQueued, isPlaying)

  private fun listener(
    type: User.Type = User.Type.User,
    download: Boolean = true,
  ) = user(
    id = "user-1",
    type = type,
    permissions = User.Permissions(
      download = download,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
  )
}
