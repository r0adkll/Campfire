// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requireServerUrl
import app.campfire.core.session.requiredUserId
import app.campfire.network.di.DownloadClient
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Ebook files on this device: in the cache for books that were only opened, and in the download
 * directory for books downloaded to read offline. Each is named for its item and format.
 *
 * The server only serves the whole file, which is fetched through the [DownloadClient] so it
 * streams to disk rather than through memory. That client carries no auth, so the account's
 * bearer token and extra headers are attached here, as the desktop offline downloader does.
 */
@SingleIn(UserScope::class)
@Inject
class EbookFiles(
  private val userSession: UserSession,
  @DownloadClient private val client: HttpClient,
  private val accountManager: AccountManager,
  private val tokenRefresher: TokenRefresher,
  private val storage: EbookStorage,
  private val dispatcherProvider: DispatcherProvider,
) {

  /** Where [itemId]'s ebook is kept once downloaded. */
  fun downloaded(itemId: LibraryItemId, format: String): Path =
    Path(storage.downloadDirectory(), fileName(itemId, format))

  /** Where [itemId]'s ebook is kept once opened, but not downloaded. */
  fun cached(itemId: LibraryItemId, format: String): Path =
    Path(storage.cacheDirectory(), fileName(itemId, format))

  /** [itemId]'s whole downloaded ebook, whatever its format, or null. */
  fun downloadedFile(itemId: LibraryItemId): Path? = filesIn(storage.downloadDirectory(), itemId)
    .firstOrNull { it.hasContent() }

  /** Every whole downloaded ebook, by item; a `.part` file is one cut short. */
  fun downloadedFiles(): Map<LibraryItemId, Path> {
    val dir = Path(storage.downloadDirectory())
    if (!SystemFileSystem.exists(dir)) return emptyMap()
    return SystemFileSystem.list(dir)
      .filter { !it.name.endsWith(".part") && it.hasContent() }
      .associateBy { it.name.substringBefore('.') }
  }

  fun deleteDownloaded(itemId: LibraryItemId) = deleteFilesIn(storage.downloadDirectory(), itemId)

  fun deleteCached(itemId: LibraryItemId) = deleteFilesIn(storage.cacheDirectory(), itemId)

  /**
   * Fetch [itemId]'s ebook to [file], through a `.part` file so a broken download never leaves a
   * file behind that looks whole. [onProgress] gets the bytes so far and the size, or -1.
   */
  suspend fun fetch(
    itemId: LibraryItemId,
    file: Path,
    onProgress: (bytes: Long, total: Long) -> Unit = { _, _ -> },
  ) = withContext(dispatcherProvider.io) {
    val userId = userSession.requiredUserId
    val serverUrl = userSession.requireServerUrl
    val url = "${serverUrl.trimEnd('/')}/api/items/$itemId/ebook"
    SystemFileSystem.createDirectories(file.parent!!)
    val partFile = Path("$file.part")

    val token = accountManager.getToken(userId)?.accessToken
    if (!fetch(url, userId, token, partFile, onProgress)) {
      val refreshed = tokenRefresher.refresh(userId, serverUrl, token)?.accessToken
        ?: throw IOException("Ebook download unauthorized and the token could not be refreshed")
      if (!fetch(url, userId, refreshed, partFile, onProgress)) {
        throw IOException("Ebook download unauthorized after a token refresh")
      }
    }
    SystemFileSystem.atomicMove(partFile, file)
    bark { "Downloaded ebook for $itemId to $file" }
  }

  /** Returns false when the server rejected [token] as unauthorized, true once the file is on disk. */
  private suspend fun fetch(
    url: String,
    userId: String,
    token: String?,
    partFile: Path,
    onProgress: (bytes: Long, total: Long) -> Unit,
  ): Boolean {
    val extraHeaders = accountManager.getExtraHeaders(userId).orEmpty()
    return client.prepareGet(url) {
      if (!token.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
      extraHeaders.forEach { (name, value) -> header(name, value) }
    }.execute { response ->
      when (response.status) {
        HttpStatusCode.OK -> {
          val total = response.contentLength() ?: -1L
          val body = response.bodyAsChannel()
          SystemFileSystem.sink(partFile).buffered().use { sink ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytes = 0L
            while (true) {
              val read = body.readAvailable(buffer)
              if (read < 0) break
              sink.write(buffer, 0, read)
              bytes += read
              onProgress(bytes, total)
            }
          }
          true
        }
        HttpStatusCode.Unauthorized -> false
        else -> throw IOException("Ebook download failed: HTTP ${response.status.value}")
      }
    }
  }

  private fun deleteFilesIn(directory: String, itemId: LibraryItemId) {
    filesIn(directory, itemId).forEach { SystemFileSystem.delete(it, mustExist = false) }
  }

  private fun filesIn(directory: String, itemId: LibraryItemId): List<Path> {
    val dir = Path(directory)
    if (!SystemFileSystem.exists(dir)) return emptyList()
    return SystemFileSystem.list(dir).filter { it.name.startsWith("$itemId.") }
  }
}

private fun fileName(itemId: LibraryItemId, format: String) = "$itemId.$format"

internal fun Path.size(): Long = SystemFileSystem.metadataOrNull(this)?.size ?: 0L

internal fun Path.hasContent(): Boolean = size() > 0L

/** Move [from] to [to], copying where they're on different file systems (desktop's temp dir). */
internal fun moveFile(from: Path, to: Path) {
  SystemFileSystem.createDirectories(to.parent!!)
  try {
    SystemFileSystem.atomicMove(from, to)
  } catch (_: IOException) {
    SystemFileSystem.source(from).buffered().use { source ->
      SystemFileSystem.sink(to).buffered().use { sink -> source.transferTo(sink) }
    }
    SystemFileSystem.delete(from)
  }
}

private const val BUFFER_SIZE = 64 * 1024
