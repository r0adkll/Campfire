// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.libraries.item.LibraryItemStore
import app.campfire.network.RequestOrigin
import app.campfire.network.models.AudioFile
import app.campfire.network.models.AudioMetaTags
import app.campfire.network.models.AudioTrack
import app.campfire.network.models.EBookFile
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.FileMetadata
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaExpanded
import app.campfire.network.models.MediaMinified
import app.campfire.network.models.MediaType
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class StoreLibraryItemRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private lateinit var db: CampfireDatabase

  @Test
  fun `a cached ebook-only book comes back without a request`() = repositoryTest { cache ->
    cache(ebookOnly(title = "Cached"))
    api.libraryItemResult = { Result.success(ebookOnly(title = "Fetched")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Cached")
    assertThat(api.libraryItemRequests).isEmpty()
  }

  @Test
  fun `an ebook-only book that isn't cached is fetched once`() = repositoryTest {
    api.libraryItemResult = { Result.success(ebookOnly(title = "Fetched")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Fetched")
    assertThat(api.libraryItemRequests).containsExactly(ITEM)
  }

  @Test
  fun `a cached audiobook with its tracks comes back without a request`() = repositoryTest { cache ->
    cache(audiobook(title = "Cached"))
    api.libraryItemResult = { Result.success(audiobook(title = "Fetched")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Cached")
    assertThat(api.libraryItemRequests).isEmpty()
  }

  @Test
  fun `an audiobook with an ebook, cached without its tracks, is fetched to play`() = repositoryTest { cache ->
    cache(audiobookFromSearch(title = "Cached"))
    api.libraryItemResult = { Result.success(audiobook(title = "Fetched")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Fetched")
    assertThat(api.libraryItemRequests).containsExactly(ITEM)
  }

  @Test
  fun `an audiobook with an ebook, cached from a library page, is fetched to play`() = repositoryTest {
    cacheSummary(audiobookSummary(title = "Cached"))
    api.libraryItemResult = { Result.success(audiobook(title = "Fetched")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Fetched")
    assertThat(api.libraryItemRequests).containsExactly(ITEM)
  }

  @Test
  fun `with the server unreachable, a cached copy without its tracks still comes back`() = repositoryTest { cache ->
    cache(audiobookFromSearch(title = "Cached"))
    api.libraryItemResult = { Result.failure(RuntimeException("Unable to resolve host")) }

    val item = repository().getLibraryItem(ITEM)

    assertThat(item.media.metadata.title).isEqualTo("Cached")
  }

  private fun TestScope.dao() = SqlDelightLibraryItemDao(
    db = db,
    urlHydrator = FakeUrlHydrator(),
    dispatcherProvider = asTestDispatcherProvider(),
    userSession = UserSession.LoggedIn(user("user")),
  )

  private fun TestScope.repository() = StoreLibraryItemRepository(
    libraryItemStoreFactory = LibraryItemStore.Factory(
      api = api,
      db = db,
      libraryItemDao = dao(),
      dispatcherProvider = asTestDispatcherProvider(),
      purger = NoPurger,
    ),
    api = api,
    purger = NoPurger,
  )

  /** Caches [item] the way a library page does: never over a fuller copy. */
  private suspend fun cacheSummary(item: LibraryItemMinified.Book) {
    db.libraryItemsQueries.insertOrIgnore(item.asDbModel("server"))
    db.mediaQueries.insertOrIgnore(item.media.asDbModel(item.id))
  }

  /** Runs [block] on a fresh database, with a way to put an item in its cache. */
  private fun repositoryTest(
    block: suspend TestScope.(cache: suspend (LibraryItemExpanded) -> Unit) -> Unit,
  ) = runTest {
    val driver = createDriver()
    try {
      db = DatabaseFactory(driver).build()
      block { item -> dao().insert(item) }
    } finally {
      driver.close()
    }
  }

  private object NoPurger : LibraryItemPurger {
    override suspend fun purge(itemIds: Collection<LibraryItemId>) = Unit
    override suspend fun purgeIfRemoved(candidates: Collection<LibraryItemId>) = emptySet<LibraryItemId>()
  }

  private companion object {
    const val ITEM = "item"

    fun ebookOnly(title: String) = book(title = title, ebookFile = EPUB)

    fun audiobook(title: String) = book(title = title, audioFiles = listOf(AUDIO_FILE), tracks = listOf(TRACK))

    /** An audiobook that comes with an ebook, in the basic shape search sends a series' books in. */
    fun audiobookFromSearch(title: String) = book(title = title, audioFiles = listOf(AUDIO_FILE), ebookFile = EPUB)

    /** An audiobook that comes with an ebook, as a library page sends it: counts but no lists. */
    fun audiobookSummary(title: String) = LibraryItemMinified.Book(
      id = ITEM,
      ino = "ino",
      libraryId = "library",
      folderId = "folder",
      path = "path",
      relPath = "relPath",
      isFile = false,
      mtimeMs = 0L,
      ctimeMs = 0L,
      birthtimeMs = 0L,
      addedAt = 0L,
      updatedAt = 0L,
      isMissing = false,
      isInvalid = false,
      mediaType = MediaType.Book,
      size = 0L,
      media = MediaMinified(
        id = "media",
        coverPath = null,
        numTracks = 1,
        numAudioFiles = 1,
        ebookFormat = "epub",
        metadata = MinifiedBookMetadata(title = title),
      ),
    )

    fun book(
      title: String,
      audioFiles: List<AudioFile> = emptyList(),
      tracks: List<AudioTrack> = emptyList(),
      ebookFile: EBookFile? = null,
    ) = LibraryItemExpanded.Book(
      id = ITEM,
      ino = "ino",
      libraryId = "library",
      folderId = "folder",
      path = "path",
      relPath = "relPath",
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
        coverPath = null,
        metadata = ExpandedBookMetadata(title = title),
        audioFiles = audioFiles,
        tracks = tracks,
        ebookFile = ebookFile,
      ),
      libraryFiles = emptyList(),
    ).apply { applyOrigin(RequestOrigin.Url("server")) }

    fun file(name: String) = FileMetadata(
      filename = name,
      ext = name.substringAfterLast('.'),
      path = "path/$name",
      relPath = name,
      size = 1L,
      mtimeMs = 0L,
      ctimeMs = 0L,
      birthtimeMs = 0L,
    )

    val EPUB = EBookFile(ino = "ebook", metadata = file("$ITEM.epub"), ebookFormat = "epub")

    val AUDIO_FILE = AudioFile(
      index = 1,
      ino = "audio",
      metadata = file("track.mp3"),
      addedAt = 0L,
      updatedAt = 0L,
      manuallyVerified = false,
      exclude = false,
      format = "MP2/3 (MPEG audio layer 2/3)",
      duration = 60f,
      bitRate = 64_000,
      codec = "mp3",
      timeBase = "1/14112000",
      channels = 2,
      channelLayout = "stereo",
      chapters = emptyList(),
      metaTags = AudioMetaTags(),
      mimeType = "audio/mpeg",
    )

    val TRACK = AudioTrack(
      index = 1,
      startOffset = 0f,
      duration = 60f,
      title = "track.mp3",
      contentUrl = "/api/items/$ITEM/file/audio",
      mimeType = "audio/mpeg",
      codec = "mp3",
      metadata = file("track.mp3"),
    )
  }
}
