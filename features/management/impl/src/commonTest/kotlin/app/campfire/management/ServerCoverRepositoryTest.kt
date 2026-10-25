// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.account.test.FakeUrlHydrator
import app.campfire.management.api.cover.BookImage
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.FileMetadata
import app.campfire.network.models.LibraryFile
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.MediaExpanded
import app.campfire.network.models.MediaType
import app.campfire.network.test.FakeAudioBookShelfApi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ServerCoverRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val repository = ServerCoverRepository(api, FakeUrlHydrator())

  @Test
  fun `a cover search asks for the terms as typed, without blanks or repeats`() = runTest {
    api.searchCoversResult = { Result.success(listOf("https://a.example/1.jpg", " ", "https://a.example/1.jpg")) }

    val covers = repository.searchCovers("audible", "  Children of Ruin ", " ").getOrThrow()

    assertThat(covers).containsExactly("https://a.example/1.jpg")
    assertThat(api.coverSearches)
      .containsExactly(FakeAudioBookShelfApi.CoverSearch("audible", "Children of Ruin", null))
  }

  @Test
  fun `the book's images are its image files, the cover among them`() = runTest {
    api.libraryItemResult = {
      Result.success(
        book(
          coverPath = "/audiobooks/Ruin/cover.jpg",
          files = listOf(
            file("ino-1", "cover.jpg", "image"),
            file("ino-2", "Children of Ruin.m4b", "audio"),
            file("ino-3", "folder.png", "image"),
          ),
        ),
      )
    }

    val images = repository.getBookImages("book").getOrThrow()

    assertThat(images).containsExactly(
      BookImage("cover.jpg", "/audiobooks/Ruin/cover.jpg", "/api/items/book/file/ino-1", 1024, isCover = true),
      BookImage("folder.png", "/audiobooks/Ruin/folder.png", "/api/items/book/file/ino-3", 1024, isCover = false),
    )
  }

  @Test
  fun `a book whose files can't be fetched has no images to offer`() = runTest {
    api.libraryItemResult = { Result.failure(IllegalStateException("offline")) }

    assertThat(repository.getBookImages("book").isFailure).isEqualTo(true)
  }

  private fun file(ino: String, name: String, type: String) = LibraryFile(
    ino = ino,
    metadata = FileMetadata(
      filename = name,
      ext = name.substringAfterLast('.'),
      path = "/audiobooks/Ruin/$name",
      relPath = name,
      size = 1024,
      mtimeMs = 0L,
      ctimeMs = 0L,
      birthtimeMs = 0L,
    ),
    addedAt = 0L,
    updatedAt = 0L,
    fileType = type,
  )

  private fun book(coverPath: String?, files: List<LibraryFile>) = LibraryItemExpanded.Book(
    id = "book",
    ino = "ino",
    libraryId = "library",
    folderId = "folder",
    path = "/audiobooks/Ruin",
    relPath = "Ruin",
    isFile = false,
    mtimeMs = 0L,
    ctimeMs = 0L,
    birthtimeMs = 0L,
    addedAt = 0L,
    updatedAt = 0L,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Book,
    media = MediaExpanded(
      id = "media",
      coverPath = coverPath,
      metadata = ExpandedBookMetadata(title = "Children of Ruin"),
    ),
    libraryFiles = files,
  )
}
