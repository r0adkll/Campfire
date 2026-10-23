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
import app.campfire.core.time.FatherTime
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.fixtures.user
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray

/**
 * Ebook files in a temporary directory, fetched from a fake server, for the reader and its
 * downloader to share as they do in the app.
 */
internal class EbookFilesFixture {

  private val root = Path(SystemTemporaryDirectory, "ebook-files-test-${Random.nextLong()}")
  val storage = object : EbookStorage {
    override fun cacheDirectory() = Path(root, "cache").toString()
    override fun downloadDirectory() = Path(root, "downloads").toString()
  }
  val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )
  val time = FakeFatherTime()
  val mediaProgress = FakeMediaProgressRepository()
  val items = FakeLibraryItemRepository().apply {
    libraryItem = libraryItem(id = ITEM, media = media(ebookFormat = "epub"))
  }

  /** What the server answers each request with, in turn; the last one repeats. */
  var responses = listOf(HttpStatusCode.OK)
  val requests = mutableListOf<Map<String, String?>>()

  private val engine = MockEngine { request ->
    requests += mapOf(
      "url" to request.url.encodedPath.substringAfter("/api/"),
      "auth" to request.headers[HttpHeaders.Authorization],
      "extra" to request.headers["X-Extra"],
    )
    val status = responses[(requests.size - 1).coerceAtMost(responses.lastIndex)]
    respond(BOOK, status, headersOf(HttpHeaders.ContentLength, BOOK.size.toString()))
  }

  /** A new instance each time, as after a restart; everything kept is on the disk. */
  fun files() = EbookFiles(
    userSession = UserSession.LoggedIn(user(id = USER)),
    client = HttpClient(engine),
    accountManager = FakeAccounts(),
    tokenRefresher = FakeRefresher(),
    storage = storage,
    dispatcherProvider = dispatchers,
  )

  fun repository(files: EbookFiles = files()) = DefaultEbookRepository(
    libraryItemRepository = items,
    mediaProgressRepository = mediaProgress,
    files = files,
  )

  fun downloader(files: EbookFiles = files()) = EbookDownloader(
    libraryItemRepository = items,
    files = files,
    dispatcherProvider = dispatchers,
    fatherTime = time,
    coroutineScopeHolder = CoroutineScopeHolder { CoroutineScope(Dispatchers.Default) },
  )

  fun cached(name: String = "$ITEM.epub") = Path(storage.cacheDirectory(), name)

  fun downloaded(name: String = "$ITEM.epub") = Path(storage.downloadDirectory(), name)

  fun readFile(path: String): String =
    SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray().decodeToString() }

  fun cleanUp() {
    listOf("cache", "downloads").forEach { dir ->
      val path = Path(root, dir)
      if (SystemFileSystem.exists(path)) SystemFileSystem.list(path).forEach { SystemFileSystem.delete(it) }
      SystemFileSystem.delete(path, mustExist = false)
    }
    SystemFileSystem.delete(root, mustExist = false)
  }

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

  class FakeFatherTime(var nowMillis: Long = 0L) : FatherTime {
    override fun now(): LocalDateTime = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC)
    override fun today(): LocalDate = now().date
    override fun nowInEpochMillis(): Long = nowMillis
  }

  companion object {
    const val USER = "user-1"
    const val ITEM = "item-1"
    val BOOK = "the book".encodeToByteArray()
  }
}
