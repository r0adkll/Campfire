// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.reader.api.EbookDownload
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray

class DefaultEbookRepositoryTest {

  private val root = Path(SystemTemporaryDirectory, "ebook-repository-test-${Random.nextLong()}")
  private val storage = object : EbookStorage {
    override fun cacheDirectory() = Path(root, "cache").toString()
    override fun downloadDirectory() = Path(root, "downloads").toString()
  }
  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )
  private val accounts = FakeAccounts()
  private val refresher = FakeRefresher()
  private val mediaProgress = FakeMediaProgressRepository()
  private val items = FakeLibraryItemRepository().apply {
    libraryItem = libraryItem(id = ITEM, media = media(ebookFormat = "epub"))
  }

  /** What the server answers each request with, in turn; the last one repeats. */
  private var responses = listOf(HttpStatusCode.OK)
  private val requests = mutableListOf<Map<String, String?>>()

  private val engine = MockEngine { request ->
    requests += mapOf(
      "url" to request.url.encodedPath.substringAfter("/api/"),
      "auth" to request.headers[HttpHeaders.Authorization],
      "extra" to request.headers["X-Extra"],
    )
    val status = responses[(requests.size - 1).coerceAtMost(responses.lastIndex)]
    respond(BOOK, status, headersOf(HttpHeaders.ContentLength, BOOK.size.toString()))
  }

  private fun repository() = DefaultEbookRepository(
    userSession = UserSession.LoggedIn(user(id = USER)),
    libraryItemRepository = items,
    mediaProgressRepository = mediaProgress,
    client = HttpClient(engine),
    accountManager = accounts,
    tokenRefresher = refresher,
    storage = storage,
    dispatcherProvider = dispatchers,
    coroutineScopeHolder = CoroutineScopeHolder { CoroutineScope(Dispatchers.Default) },
  )

  @AfterTest
  fun cleanUp() {
    listOf("cache", "downloads").forEach { dir ->
      val path = Path(root, dir)
      if (SystemFileSystem.exists(path)) SystemFileSystem.list(path).forEach { SystemFileSystem.delete(it) }
      SystemFileSystem.delete(path, mustExist = false)
    }
    SystemFileSystem.delete(root, mustExist = false)
  }

  @Test
  fun open_fetchesTheBookOnce_withTheAccountsCredentials() = runTest {
    val repository = repository()

    val first = repository.open(ITEM).getOrThrow()
    val second = repository.open(ITEM).getOrThrow()

    assertThat(first.filePath).isEqualTo(Path(storage.cacheDirectory(), "$ITEM.epub").toString())
    assertThat(second.filePath).isEqualTo(first.filePath)
    assertThat(readFile(first.filePath)).isEqualTo(BOOK.decodeToString())
    assertThat(requests).containsExactly(
      mapOf("url" to "items/$ITEM/ebook", "auth" to "Bearer access", "extra" to "value"),
    )
  }

  @Test
  fun open_refreshesAnExpiredTokenOnce() = runTest {
    responses = listOf(HttpStatusCode.Unauthorized, HttpStatusCode.OK)

    repository().open(ITEM).getOrThrow()

    assertThat(requests.map { it["auth"] }).containsExactly("Bearer access", "Bearer refreshed")
  }

  @Test
  fun open_failsOnAServerError_withoutLeavingAFileBehind() = runTest {
    responses = listOf(HttpStatusCode.InternalServerError)

    val result = repository().open(ITEM)

    assertThat(result.isFailure).isTrue()
    assertThat(SystemFileSystem.exists(Path(storage.cacheDirectory(), "$ITEM.epub"))).isFalse()
  }

  @Test
  fun download_movesABookAlreadyOpened_andOpenPrefersIt() = runTest {
    val repository = repository()
    repository.open(ITEM).getOrThrow()

    repository.download(ITEM)
    val download = repository.observeDownload(ITEM).first { it.state == EbookDownload.State.Downloaded }

    assertThat(download.totalBytes).isEqualTo(BOOK.size.toLong())
    assertThat(requests.size).isEqualTo(1)
    assertThat(repository.open(ITEM).getOrThrow().filePath)
      .isEqualTo(Path(storage.downloadDirectory(), "$ITEM.epub").toString())
  }

  @Test
  fun download_fetchesABookNotOpened_andSurvivesARestart() = runTest {
    repository().apply {
      download(ITEM)
      observeDownload(ITEM).first { it.state == EbookDownload.State.Downloaded }
    }

    // A new session reads what's downloaded from the disk
    val restarted = repository().observeDownload(ITEM).first()
    assertThat(restarted.state).isEqualTo(EbookDownload.State.Downloaded)
  }

  @Test
  fun download_reportsAFailure() = runTest {
    responses = listOf(HttpStatusCode.Forbidden)
    val repository = repository()

    repository.download(ITEM)

    assertThat(repository.observeDownload(ITEM).first { it.state != EbookDownload.State.Downloading }.state)
      .isEqualTo(EbookDownload.State.Failed)
  }

  @Test
  fun deleting_removesTheDownload_andAPurgeRemovesEveryCopy() = runTest {
    val repository = repository()
    repository.download(ITEM)
    repository.observeDownload(ITEM).first { it.state == EbookDownload.State.Downloaded }
    repository.deleteDownload(ITEM)
    assertThat(repository.observeDownload(ITEM).first().state).isEqualTo(EbookDownload.State.None)

    repository.open(ITEM).getOrThrow()
    repository.deleteFiles(ITEM)

    assertThat(SystemFileSystem.exists(Path(storage.cacheDirectory(), "$ITEM.epub"))).isFalse()
  }

  @Test
  fun saveProgress_savesThroughTheProgressRepository() = runTest {
    repository().saveProgress(ITEM, "epubcfi(/6/4!/4/2/1:0)", 0.4f)

    assertThat(mediaProgress.invocations).containsExactly(
      FakeMediaProgressRepository.Invocation.UpdateEbookProgress(ITEM, "epubcfi(/6/4!/4/2/1:0)", 0.4f),
    )
  }

  private fun readFile(path: String): String =
    SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray().decodeToString() }

  private class FakeAccounts : AccountManager {
    override suspend fun addAccount(
      serverUrl: String,
      accessToken: String,
      refreshToken: String?,
      extraHeaders: Map<String, String>?,
      user: User,
      activate: Boolean,
    ) = Unit
    override suspend fun invalidateAccount(user: User) = Unit
    override suspend fun switchAccount(user: User) = Unit
    override suspend fun logout(server: Server) = Unit
    override suspend fun getToken(userId: UserId): AbsToken = AbsToken("access", "refresh")
    override suspend fun updateToken(userId: UserId, newToken: AbsToken) = Unit
    override suspend fun getExtraHeaders(userId: UserId): Map<String, String> = mapOf("X-Extra" to "value")
    override suspend fun setExtraHeaders(userId: UserId, headers: Map<String, String>) = Unit
    override fun observeExtraHeaders(userId: UserId): Flow<Map<String, String>> = emptyFlow()
  }

  private class FakeRefresher : TokenRefresher {
    override suspend fun refresh(userId: UserId, serverUrl: String, staleAccessToken: String?): AbsToken =
      AbsToken("refreshed", "refresh")
  }

  private companion object {
    const val USER = "user-1"
    const val ITEM = "item-1"
    val BOOK = "the book".encodeToByteArray()
  }
}
